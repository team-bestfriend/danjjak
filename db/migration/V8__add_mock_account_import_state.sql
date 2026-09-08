-- 모의 본인 계좌 후보와 사용자가 불러온 계좌를 구분한다.

ALTER TABLE bank_accounts
    ADD COLUMN imported_at DATETIME NULL AFTER is_primary;

-- 이미 카카오 계정에 연결된 사용자의 기존 기본 계좌만 불러온 상태로 보존한다.
UPDATE bank_accounts ba
JOIN users u ON u.user_id = ba.user_id
SET ba.imported_at = ba.created_at
WHERE ba.registered_person_id IS NULL
  AND ba.is_primary = TRUE
  AND u.kakao_user_id IS NOT NULL;

-- 첫 이용 사용자는 후보를 직접 선택한 뒤 기본 계좌가 정해지도록 한다.
UPDATE bank_accounts
SET is_primary = FALSE
WHERE registered_person_id IS NULL
  AND imported_at IS NULL;

ALTER TABLE bank_accounts
    ADD CONSTRAINT chk_bank_accounts_recipient_not_imported
        CHECK (registered_person_id IS NULL OR imported_at IS NULL),
    ADD CONSTRAINT chk_bank_accounts_primary_imported
        CHECK (is_primary = FALSE OR imported_at IS NOT NULL);
