-- =====================================================================
-- V7 - Catálogo inicial de proveedores (Argentina)
-- Catálogo global (user_id NULL). Los dominios son orientativos:
-- revisarlos antes de usarlos para detectar facturas en Gmail.
-- =====================================================================

INSERT INTO providers (name, code, category, website, email_domains) VALUES
    ('EDEA',                 'EDEA',        'ELECTRICITY',  'https://www.edeaweb.com.ar', ARRAY['edeaweb.com.ar']),
    ('Edenor',               'EDENOR',      'ELECTRICITY',  'https://www.edenor.com',     ARRAY['edenor.com']),
    ('Edesur',               'EDESUR',      'ELECTRICITY',  'https://www.edesur.com.ar',  ARRAY['edesur.com.ar']),
    ('Coopelectric',         'COOPELECTRIC','ELECTRICITY',  'https://www.coopelectric.com.ar', ARRAY['coopelectric.com.ar']),
    ('Camuzzi Gas Pampeana', 'CAMUZZI',     'GAS',          'https://www.camuzzigas.com', ARRAY['camuzzigas.com.ar']),
    ('Metrogas',             'METROGAS',    'GAS',          'https://www.metrogas.com.ar',ARRAY['metrogas.com.ar']),
    ('Naturgy',              'NATURGY',     'GAS',          'https://www.naturgy.com.ar', ARRAY['naturgy.com.ar']),
    ('AySA',                 'AYSA',        'WATER',        'https://www.aysa.com.ar',    ARRAY['aysa.com.ar']),
    ('OSSE',                 'OSSE',        'WATER',        'https://www.osmgp.gov.ar',   ARRAY['osmgp.gov.ar']),
    ('Personal / Fibertel',  'PERSONAL',    'INTERNET',     'https://www.personal.com.ar',ARRAY['personal.com.ar', 'telecom.com.ar']),
    ('Movistar',             'MOVISTAR',    'PHONE',        'https://www.movistar.com.ar',ARRAY['movistar.com.ar']),
    ('Claro',                'CLARO',       'PHONE',        'https://www.claro.com.ar',   ARRAY['claro.com.ar']),
    ('Telecentro',           'TELECENTRO',  'INTERNET',     'https://www.telecentro.com.ar', ARRAY['telecentro.net.ar']),
    ('DirecTV',              'DIRECTV',     'TV',           'https://www.directv.com.ar', ARRAY['directvla.com.ar']),
    ('ARCA',                 'ARCA',        'TAX',          'https://www.arca.gob.ar',    ARRAY['arca.gob.ar']),
    ('ARBA',                 'ARBA',        'TAX',          'https://www.arba.gov.ar',    ARRAY['arba.gov.ar']),
    ('Netflix',              'NETFLIX',     'SUBSCRIPTION', 'https://www.netflix.com',    ARRAY['netflix.com']),
    ('Spotify',              'SPOTIFY',     'SUBSCRIPTION', 'https://www.spotify.com',    ARRAY['spotify.com']),
    ('Otro',                 'OTHER',       'OTHER',        NULL,                         NULL);
