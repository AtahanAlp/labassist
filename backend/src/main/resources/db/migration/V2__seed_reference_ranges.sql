-- Canonical reference ranges (illustrative adult values). The backend uses these
-- to flag abnormal/critical analytes regardless of what the device reports.
-- Hemoglobin is sex-specific to demonstrate range selection by patient sex.

insert into reference_range (id, code, name, unit, sex, age_min, age_max, low, high, critical_low, critical_high)
values
    (gen_random_uuid(), 'GLU',  'Glucose, fasting',          'mg/dL',   null, null, null, 70,   99,   40,   500),
    (gen_random_uuid(), 'NA',   'Sodium',                    'mmol/L',  null, null, null, 136,  145,  120,  160),
    (gen_random_uuid(), 'K',    'Potassium',                 'mmol/L',  null, null, null, 3.5,  5.1,  2.5,  6.5),
    (gen_random_uuid(), 'CL',   'Chloride',                  'mmol/L',  null, null, null, 98,   107,  null, null),
    (gen_random_uuid(), 'CREA', 'Creatinine',                'mg/dL',   null, null, null, 0.7,  1.3,  null, 7.4),
    (gen_random_uuid(), 'BUN',  'Urea Nitrogen',             'mg/dL',   null, null, null, 7,    20,   null, 100),
    (gen_random_uuid(), 'HGB',  'Hemoglobin',                'g/dL',    'M',  null, null, 13.5, 17.5, 7.0,  20.0),
    (gen_random_uuid(), 'HGB',  'Hemoglobin',                'g/dL',    'F',  null, null, 12.0, 15.5, 7.0,  20.0),
    (gen_random_uuid(), 'WBC',  'Leukocytes',                '10^3/uL', null, null, null, 4.5,  11.0, 1.0,  30.0),
    (gen_random_uuid(), 'PLT',  'Platelets',                 '10^3/uL', null, null, null, 150,  400,  20,   1000),
    (gen_random_uuid(), 'ALT',  'Alanine Aminotransferase',  'U/L',     null, null, null, 7,    56,   null, 1000),
    (gen_random_uuid(), 'CRP',  'C-Reactive Protein',        'mg/L',    null, null, null, 0,    5,    null, null),
    (gen_random_uuid(), 'TSH',  'Thyrotropin',               'mIU/L',   null, null, null, 0.4,  4.0,  null, null);
