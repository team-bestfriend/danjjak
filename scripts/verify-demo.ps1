param([ValidateRange(1024, 65535)][int]$Port = 23307)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$containerName = 'danjjak-check-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$containerId = $null
$environmentNames = @('DANJJAK_DB_INTEGRATION_TEST', 'DANJJAK_DB_URL', 'DANJJAK_DB_USERNAME', 'DANJJAK_DB_PASSWORD', 'DANJJAK_VOICE_DIR')
$previousEnvironment = @{}
foreach ($name in $environmentNames) { $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }

function Assert-Exit([string]$Task) {
    if ($LASTEXITCODE -ne 0) { throw "$Task failed (exit $LASTEXITCODE)" }
}

try {
    # 매번 새 컨테이너만 만들며 기존 시연 DB와 카카오 연결은 건드리지 않는다.
    $containerId = docker create --name $containerName --publish "127.0.0.1:${Port}:3306" --env MYSQL_ROOT_PASSWORD=temporary_test_only --env MYSQL_DATABASE=danjjak --env MYSQL_USER=danjjak --env MYSQL_PASSWORD=danjjak_local mysql:8.4.11 --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci
    Assert-Exit 'Create isolated MySQL'
    $containerId = "$containerId".Trim()
    docker start $containerId | Out-Null
    Assert-Exit 'Start isolated MySQL'
    Write-Output "Waiting for isolated MySQL on port $Port..."
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        docker exec --env MYSQL_PWD=danjjak_local $containerId mysql '--protocol=TCP' '--host=127.0.0.1' '--user=danjjak' '--execute=SELECT 1' danjjak 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (!$ready) { throw 'Isolated MySQL did not become ready' }

    $flyway = @('run', '--rm', '--network', "container:$containerId", '--mount', "type=bind,source=$repo\db\migration,target=/flyway/sql,readonly", 'flyway/flyway:13.4.0', '-url=jdbc:mysql://127.0.0.1:3306/danjjak?allowPublicKeyRetrieval=true&useSSL=false', '-user=danjjak', '-password=danjjak_local')
    & docker @flyway migrate
    Assert-Exit 'Migrate fresh database'
    & docker @flyway validate
    Assert-Exit 'Validate migration checksums'

    $seedSql = @'
SELECT COUNT(*) FROM users WHERE kakao_user_id IS NULL AND consent_completed=0 AND usage_log_agreed=0 AND guardian_share_agreed=0;
SELECT COUNT(*) FROM bank_accounts WHERE registered_person_id IS NULL AND balance IN (50000000,30000000);
SELECT COUNT(*) FROM registered_persons;
SELECT COUNT(*) FROM bank_accounts WHERE registered_person_id IS NOT NULL;
SELECT COUNT(*) FROM financial_patterns WHERE is_active=1;
SELECT COUNT(DISTINCT transaction_category) FROM transactions WHERE transaction_category IN ('PENSION','MANAGEMENT_FEE','UTILITY_BILL');
SELECT COUNT(*) FROM pattern_executions;
SELECT COUNT(*) FROM step_execution_logs;
SELECT COUNT(*) FROM financial_patterns WHERE voice_file_path IS NOT NULL;
SELECT COUNT(*) FROM pattern_steps WHERE voice_file_path IS NOT NULL;
'@
    $seedResult = $seedSql | docker exec -i --env MYSQL_PWD=danjjak_local $containerId mysql -udanjjak --batch --skip-column-names danjjak
    Assert-Exit 'Read seed state'
    if (($seedResult -join ',') -ne '1,2,2,2,8,3,0,0,0,0') { throw "Unexpected seed state: $($seedResult -join ',')" }
    Write-Output 'Seed verified: one unbound user, two owned accounts, two recipients, eight shortcuts, three categories, no consent/logs/audio.'

    $env:DANJJAK_DB_INTEGRATION_TEST = 'true'
    $env:DANJJAK_DB_URL = "jdbc:mysql://127.0.0.1:$Port/danjjak?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false"
    $env:DANJJAK_DB_USERNAME = 'danjjak'
    $env:DANJJAK_DB_PASSWORD = 'danjjak_local'
    $env:DANJJAK_VOICE_DIR = Join-Path $repo 'backend/build/integration-voices'
    Push-Location (Join-Path $repo 'backend')
    try {
        # 매 실행마다 새 DB를 사용하므로 Gradle의 이전 성공 결과를 재사용하지 않는다.
        & .\gradlew.bat test war --rerun-tasks
        Assert-Exit 'Backend integration tests and WAR'
    } finally { Pop-Location }
    Push-Location (Join-Path $repo 'frontend')
    try {
        & npm.cmd test -- --test-reporter=dot
        Assert-Exit 'Frontend tests'
        & npm.cmd run build
        Assert-Exit 'Frontend build'
    } finally { Pop-Location }
    Write-Output 'Repeatable demo verification passed. Real microphone, voice quality, and external login require the manual checklist.'
} finally {
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process') }
    # 이 실행에서 생성한 정확한 컨테이너 ID와 그 익명 볼륨만 정리한다.
    if ($containerId -match '^[a-f0-9]{64}$') { docker rm --force --volumes $containerId | Out-Null }
}
