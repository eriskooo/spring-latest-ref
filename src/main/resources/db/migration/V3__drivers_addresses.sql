-- Create DRIVER and ADDRESS tables and seed data linked to AUTOMOBILs

CREATE TABLE IF NOT EXISTS DRIVER
(
    id
    IDENTITY
    PRIMARY
    KEY,
    automobil_id
    BIGINT
    NOT
    NULL,
    name
    VARCHAR
(
    255
) NOT NULL,
    surname VARCHAR
(
    255
) NOT NULL
    ,
    CONSTRAINT fk_driver_automobil
    FOREIGN KEY
(
    automobil_id
) REFERENCES AUTOMOBIL
(
    id
)
    ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_driver_auto ON DRIVER(automobil_id);

CREATE TABLE IF NOT EXISTS ADDRESS
(
    id
    IDENTITY
    PRIMARY
    KEY,
    driver_id
    BIGINT
    NOT
    NULL,
    street
    VARCHAR
(
    255
) NOT NULL,
    city VARCHAR
(
    255
) NOT NULL
    ,
    CONSTRAINT fk_address_driver
    FOREIGN KEY
(
    driver_id
) REFERENCES DRIVER
(
    id
)
    ON DELETE CASCADE
    ,
    CONSTRAINT uq_address_driver UNIQUE
(
    driver_id
)
    );

CREATE INDEX IF NOT EXISTS idx_address_driver ON ADDRESS(driver_id);

-- Seed data: generate 200 drivers and 200 addresses
-- Rule: each car has 1–3 drivers deterministically by automobil_id % 3
-- automobil_id % 3 = 0 -> 1 driver, = 1 -> 2 drivers, = 2 -> 3 drivers

-- Insert drivers with deterministic IDs using ROW_NUMBER over ordered set
INSERT INTO DRIVER (id, automobil_id, name, surname)
SELECT ROW_NUMBER()             OVER (ORDER BY a.id, n.n) AS id, a.id AS automobil_id,
       'Driver' || ROW_NUMBER() OVER (ORDER BY a.id, n.n) AS name, 'Surname' || a.id AS surname
FROM AUTOMOBIL a
         JOIN (SELECT 1 AS n
               UNION ALL
               SELECT 2
               UNION ALL
               SELECT 3) n ON n.n <= CASE MOD(a.id, 3)
                                         WHEN 0 THEN 1
                                         WHEN 1 THEN 2
                                         ELSE 3
    END
ORDER BY a.id, n.n;

-- Insert one address per driver (one-to-one)
INSERT INTO ADDRESS (id, driver_id, street, city)
SELECT d.id                     AS id,
       d.id                     AS driver_id,
       'Street ' || d.id        AS street,
       'City ' || MOD(d.id, 50) AS city
FROM DRIVER d;
