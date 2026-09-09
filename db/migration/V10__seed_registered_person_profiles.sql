UPDATE registered_persons
SET profile_image_key = CASE relationship
    WHEN '아들' THEN 'adult_man'
    WHEN '딸' THEN 'adult_woman'
    ELSE profile_image_key
END
WHERE profile_image_key IS NULL
  AND relationship IN ('아들', '딸');
