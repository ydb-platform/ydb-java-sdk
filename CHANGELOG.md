## 1.9.3 ##

* Fix String comparsion in CloudAuthHelper.

## 1.9.2 ##

* Endpoint re-discovery now launches periodically as intended.

## 1.9.1 ##

* All timers are now stopped on shutdown so the application should stop immediately.

## 1.9.0 ##

* Added Random choice load balancer which is now used by default instead of round robin load balancer.

## 1.8.4 ##

* Use a bit more modern GRPC name resolver factory API to facilitate GRPC implementation upgrades downstream.

## 1.8.3 ##

* Added connection string support.

## 1.8.2 ##

* Added CloudAuthProvider for cloud authentication with environment variables.

## 1.8.1 ##

* Added AlterLogTable to ydb_logstore_v1.proto.

## 1.8.0 ##

* Added fast backoff settings to SessionRetryContext. ABORTED and UNAVAILABLE status codes are retried with fast backoff now.

## 1.7.10 ##

* Update junit from 4.12 to 4.13.1.
* Update protobuf from 3.6.1 to 4.15.6.

## 1.7.9 ##

* Disabling client query cache does not disable server-side query cache anymore.

## 1.7.8 ##

* Start initial changelog.
