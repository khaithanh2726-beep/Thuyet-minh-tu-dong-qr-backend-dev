INSERT INTO exhibits (id, image_url, is_active)
VALUES (1, NULL, TRUE) AS incoming
ON DUPLICATE KEY UPDATE
    image_url = incoming.image_url,
    is_active = incoming.is_active;

INSERT INTO exhibit_translations
    (id, exhibit_id, language_code, title, content)
VALUES
    (
        1,
        1,
        'vi-vn',
        'Đèn dầu cổ',
        'Đèn dầu được sử dụng để thắp sáng trước khi đèn điện phổ biến.'
    ) AS incoming
ON DUPLICATE KEY UPDATE
    exhibit_id = incoming.exhibit_id,
    title = incoming.title,
    content = incoming.content,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO exhibit_translations
    (id, exhibit_id, language_code, title, content)
VALUES
    (
        2,
        1,
        'en-us',
        'Antique oil lamp',
        'Oil lamps were used for lighting before electric lamps became common.'
    ) AS incoming
ON DUPLICATE KEY UPDATE
    exhibit_id = incoming.exhibit_id,
    title = incoming.title,
    content = incoming.content,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO qr_codes (id, exhibit_id, code, is_active)
VALUES (1, 1, 'DEV-DEMO-DEN-DAU-001', TRUE) AS incoming
ON DUPLICATE KEY UPDATE
    exhibit_id = incoming.exhibit_id,
    is_active = incoming.is_active;