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
    );

CREATE INDEX IF NOT EXISTS idx_address_driver ON ADDRESS(driver_id);

-- Seed data: drivers for each automobile and addresses for each driver
-- Explicit IDs are used to make cross-references deterministic in H2

-- Drivers
INSERT INTO DRIVER (id, automobil_id, name, surname)
VALUES (1, 1, 'John', 'Doe');
INSERT INTO DRIVER (id, automobil_id, name, surname)
VALUES (2, 1, 'Jane', 'Doe');
INSERT INTO DRIVER (id, automobil_id, name, surname)
VALUES (3, 2, 'Max', 'Mustermann');
INSERT INTO DRIVER (id, automobil_id, name, surname)
VALUES (4, 3, 'Anna', 'Novak');

-- Addresses
INSERT INTO ADDRESS (id, driver_id, street, city)
VALUES (1, 1, 'Main Street 1', 'Springfield');
INSERT INTO ADDRESS (id, driver_id, street, city)
VALUES (2, 1, 'Second Ave 22', 'Springfield');
INSERT INTO ADDRESS (id, driver_id, street, city)
VALUES (3, 2, 'Oak Road 5', 'Shelbyville');
INSERT INTO ADDRESS (id, driver_id, street, city)
VALUES (4, 3, 'Hauptstrasse 10', 'Berlin');
INSERT INTO ADDRESS (id, driver_id, street, city)
VALUES (5, 4, 'Národní 1', 'Prague');
