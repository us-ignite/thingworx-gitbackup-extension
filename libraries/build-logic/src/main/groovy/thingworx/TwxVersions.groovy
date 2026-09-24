package thingworx

/** Single source of truth for ThingWorx version dimensions. */
class TwxVersions {
    static final List<String> ALL = ['9.6', '9.7', '10.0', '10.1']
    static final String DEFAULT = '10.1'
    static final String RUNTIME_DEFAULT = '9.6'

    static final Map<String, String> SDK_ZIPS = [
        '9.6' : 'MED-61098-CD-096_9-6-0_ThingWorx-Extension-SDK-9-6-0.zip',
        '9.7' : 'MED-61098-CD-097_9-7-0_ThingWorx-Extension-SDK-9-7-0.zip',
        '10.0': 'MED-61098-CD-100_10-0-0_ThingWorx-Extension-SDK-10-0-0.zip',
        '10.1': 'MED-61098-CD-101_10-1-0_ThingWorx-Extension-SDK-10-1-0.zip'
    ]

    static final Map<String, Map<String, String>> SDK_CONFIGS = [
        '9.6' : [
            minTwVersion: '9.6.0',
            outputSuffix: '9.6.x',
            dbInitImage: 'ghcr.io/us-ignite/thingworx/postgres-init:9.6.9-openjdk',
            platformImage: 'ghcr.io/us-ignite/thingworx/platform-postgres:9.6.9-openjdk',
            postgresImage: 'postgres:15',
            postgresHostAuth: 'trust',
            postgresPasswordEncryption: 'md5'
        ],
        '9.7' : [
            minTwVersion: '9.7.0',
            outputSuffix: '9.7.x',
            dbInitImage: 'ghcr.io/us-ignite/thingworx/postgres-init:9.7.6-openjdk',
            platformImage: 'ghcr.io/us-ignite/thingworx/platform-postgres:9.7.6-openjdk',
            postgresImage: 'postgres:15',
            postgresHostAuth: 'trust',
            postgresPasswordEncryption: 'md5'
        ],
        '10.0': [
            minTwVersion: '10.0.0',
            outputSuffix: '10.0.x',
            dbInitImage: 'ghcr.io/us-ignite/thingworx/postgres-init:10.0.4-openjdk',
            platformImage: 'ghcr.io/us-ignite/thingworx/platform-postgres:10.0.4-openjdk',
            postgresImage: 'postgres:16',
            postgresHostAuth: 'trust',
            postgresPasswordEncryption: 'scram-sha-256'
        ],
        '10.1': [
            minTwVersion: '10.1.0',
            outputSuffix: '10.1.x',
            dbInitImage: 'ghcr.io/us-ignite/thingworx/postgres-init:10.1.2-openjdk',
            platformImage: 'ghcr.io/us-ignite/thingworx/platform-postgres:10.1.2-openjdk',
            postgresImage: 'postgres:16',
            postgresHostAuth: 'trust',
            postgresPasswordEncryption: 'scram-sha-256'
        ]
    ]

    static String sanitize(String ver) {
        ver.replace('.', '')
    }
}
