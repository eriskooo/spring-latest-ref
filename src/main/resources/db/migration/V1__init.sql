CREATE TABLE IF NOT EXISTS AUTOMOBIL
(
    id
    IDENTITY
    PRIMARY
    KEY,
    brand
    VARCHAR
(
    255
) NOT NULL,
    model VARCHAR
(
    255
) NOT NULL,
    year_made INT NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_auto_brand ON AUTOMOBIL(brand);
