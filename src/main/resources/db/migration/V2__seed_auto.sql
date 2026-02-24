-- Populate at least 100 cars deterministically (H2 syntax)
INSERT INTO AUTOMOBIL (brand, model, year_made)
SELECT 'Brand' || X      AS brand,
       'Model' || X      AS model,
       1990 + MOD(X, 35) AS year_made
FROM SYSTEM_RANGE(1, 100);
