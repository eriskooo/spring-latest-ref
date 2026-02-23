-- Create DRIVER and ADDRESS tables and seed minimal data

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
    );

CREATE INDEX IF NOT EXISTS idx_address_driver ON ADDRESS(driver_id);

-- Seed data: one driver for the first automobile and one address for the driver
INSERT INTO DRIVER (automobil_id, name, surname)
VALUES (1, 'John', 'Doe');

INSERT INTO ADDRESS (driver_id, street, city)
VALUES (1, 'Main Street 1', 'Springfield');
