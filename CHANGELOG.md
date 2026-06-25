## [0.5.0](https://github.com/AlexTesta00/aeroguard-mas/compare/v0.4.0...v0.5.0) (2026-06-25)

### Features

* **replanning:** added kotlin docs for replanning ([62dbf9b](https://github.com/AlexTesta00/aeroguard-mas/commit/62dbf9b6acc3599dd78ce4937aa7d32d7624ddb6))

## [0.4.0](https://github.com/AlexTesta00/aeroguard-mas/compare/v0.3.0...v0.4.0) (2026-06-24)

### Features

* **gui:** visualize routes and waypoints ([533b591](https://github.com/AlexTesta00/aeroguard-mas/commit/533b591fe4e627ff3d4b997ae20967643928a86c))
* **planning:** prevent secondary conflicts during resolution ([9d2198d](https://github.com/AlexTesta00/aeroguard-mas/commit/9d2198d1952559e96cc0f4febe73ec8c27ba5950))
* **scenarios:** added reak-world inspired conflict scenarios ([127ad36](https://github.com/AlexTesta00/aeroguard-mas/commit/127ad36a14285eea391f60549ca710cff18b90ca))

### Bug Fixes

* **gui:** fix default scenarios .jsonl ([15feb05](https://github.com/AlexTesta00/aeroguard-mas/commit/15feb05d695a5ac487e4e63a4b81c651ba8cb230))
* **replanning:** route aircraft to lateral weather bypass in weather situation ([892d832](https://github.com/AlexTesta00/aeroguard-mas/commit/892d832d828a41e0e2a9b5318b8166ac81c8dbda))

## [0.3.0](https://github.com/AlexTesta00/aeroguard-mas/compare/v0.2.0...v0.3.0) (2026-06-23)

### Features

* **cli:** include weather replanning scenarios in run ([e9c2da4](https://github.com/AlexTesta00/aeroguard-mas/commit/e9c2da414d0fd0210024f7384b692afd487f4493))
* **cli:** run scenarios with managed simulation ([6d30a56](https://github.com/AlexTesta00/aeroguard-mas/commit/6d30a5611ff7a881de4187eb8a52f3f6d480d04b))
* **events:** added weather replanning events ([a355177](https://github.com/AlexTesta00/aeroguard-mas/commit/a35517785045b12a88fded929ce06b57a1b6e253))
* **events:** record additional replanning events ([232948d](https://github.com/AlexTesta00/aeroguard-mas/commit/232948dd9fe6bfcf32cee0c74e8701b67e8d148a))
* **explanation:** explain weather replanning detection ([320df86](https://github.com/AlexTesta00/aeroguard-mas/commit/320df86ffd7ef65c66cb3ee2f1e457ecc6c83180))
* **gui:** add sample replay events ([d947293](https://github.com/AlexTesta00/aeroguard-mas/commit/d9472936c78fce5c9c4ba4a44e7b490475b1ec67))
* **gui:** added JSONL event validator ([2ef63fc](https://github.com/AlexTesta00/aeroguard-mas/commit/2ef63fc2b64e6a6f6281e169d695011ef24d477d))
* **gui:** added Streamlit app with event loading ([d9bd494](https://github.com/AlexTesta00/aeroguard-mas/commit/d9bd4942745d0e0e131aea746b5c147ce7cd2ff0))
* **gui:** improve aircraft symbols and altitude scaling ([82f336b](https://github.com/AlexTesta00/aeroguard-mas/commit/82f336b32babdf8909a60303d6b57ea02b926233))
* **gui:** validate weather replanning events ([0fab474](https://github.com/AlexTesta00/aeroguard-mas/commit/0fab474e7fde19c3aa2ac9c9444556be2de7ddac))
* **gui:** visualize applied manuvers and separation ([aa02203](https://github.com/AlexTesta00/aeroguard-mas/commit/aa02203d0840584d4ee4c0efaa6bb800947d9b97))
* **replanning:** added weather replanning service ([af4998d](https://github.com/AlexTesta00/aeroguard-mas/commit/af4998d2896a05b020b8d0a38b0f1e4c1143c164))
* **simulation:** apply manuvers to simulation state ([9519841](https://github.com/AlexTesta00/aeroguard-mas/commit/9519841bfa6381b8b777f22e33ab2151d7e9ae7e))
* **simulation:** run managed simulation with planned maneuvers ([711320f](https://github.com/AlexTesta00/aeroguard-mas/commit/711320fbabb3fe2ed23a0f03bb6c814d799c5e7c))

### Bug Fixes

* **gui:** fix list events bug ([d017349](https://github.com/AlexTesta00/aeroguard-mas/commit/d01734959dd5e082e6c711b41fd78a713d49dd0a))

## [0.2.0](https://github.com/AlexTesta00/aeroguard-mas/compare/v0.1.0...v0.2.0) (2026-06-22)

### Features

* **cli:** parse events output option ([4ed27fc](https://github.com/AlexTesta00/aeroguard-mas/commit/4ed27fc5cef39953d5119da2702fd5a53de12d69))
* **events:** added simulation event model ([0137fdd](https://github.com/AlexTesta00/aeroguard-mas/commit/0137fdd24480341d2d63a9f55abd754cc6819580))
* **events:** added simulation event sinks ([c71aeb7](https://github.com/AlexTesta00/aeroguard-mas/commit/c71aeb7b9b86c3ee247c168260c3e4d31304e84b))
* **events:** record simulation runs as events ([b9d5c0d](https://github.com/AlexTesta00/aeroguard-mas/commit/b9d5c0df0c1e04845be0e0c24887dec89ff21f93))
* **events:** serialize simulation events to JSON ([74eefe2](https://github.com/AlexTesta00/aeroguard-mas/commit/74eefe25ffde2a522fb20dae07c8592c47e60845))
* **explanation:** add decision explanation service ([81b8da1](https://github.com/AlexTesta00/aeroguard-mas/commit/81b8da10c0e670f3dd7036a5a7651cfe225bf251))

## [0.1.0](https://github.com/AlexTesta00/aeroguard-mas/compare/v0.0.1...v0.1.0) (2026-06-20)

### Features

* **cli:** print STRIPS resolution plan ([10aa565](https://github.com/AlexTesta00/aeroguard-mas/commit/10aa565d7171e93a79b949d7d0e79d3b702068ec))
* **cli:** show Jason agent summary ([7a477fb](https://github.com/AlexTesta00/aeroguard-mas/commit/7a477fbd68048bdf889e21656fb743ca5a5d768d))
* **jason:** added agent source catalog ([17952c4](https://github.com/AlexTesta00/aeroguard-mas/commit/17952c4b7d8b217db9bac447633634ee851c1050))
* **jason:** added AgentSpeak BDI agents ([91210b2](https://github.com/AlexTesta00/aeroguard-mas/commit/91210b21c3157de39128d7adf581a82b3ca18b35))
* **jason:** added smoke check entry point ([83ccf60](https://github.com/AlexTesta00/aeroguard-mas/commit/83ccf60d708a197da0e7f7841234265a588cf230))
* **jason:** added smoke report model ([83afe77](https://github.com/AlexTesta00/aeroguard-mas/commit/83afe775cccea53153b96bb4525736050c90f141))
* **jason:** analyze BDI concepts and delegation ([4eb7eec](https://github.com/AlexTesta00/aeroguard-mas/commit/4eb7eec737e113dc594b2bd0ccda22a593b6a0b0))
* **planning:** add STRIPS actions and problems ([abe0f27](https://github.com/AlexTesta00/aeroguard-mas/commit/abe0f27e85eccf8b56bb928111a04abbfe87c86d))
* **planning:** added STRIPS position model ([fec8081](https://github.com/AlexTesta00/aeroguard-mas/commit/fec8081d704f88fd05ade458bff95baaf9a06da6))
* **planning:** define resolution planner contract ([a6e2f4f](https://github.com/AlexTesta00/aeroguard-mas/commit/a6e2f4f1333ef41ada291954186165ce49729816))
* **planning:** implement BFS STRIPS planner ([5c421dc](https://github.com/AlexTesta00/aeroguard-mas/commit/5c421dc05f2acdf727c1622d325717eb6a3fcb55))
* **planning:** resolve conflicts with STRIPS planner ([adac42e](https://github.com/AlexTesta00/aeroguard-mas/commit/adac42e5ab2b8fa3da99e9429a69364a67266d1e))
