echo 'Running appbase-stacktest: appbase-core REST API curl tests...'


##############################
### Configuration
##############################
echo "Testing appbase-core: Configuration REST API"

curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/configurationListeners' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/globalConfigurationListeners' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/nonValidatingTypes' \
  -H 'accept: application/json'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/nonValidatingTypes' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/propertyDeserializers/configurationTypes' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/propertyDeserializers' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/secureFields/configurationType' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/securePropertyKeys' \
  -H 'accept: application/json'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/securePropertyKeys' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/secureTypes' \
  -H 'accept: application/json'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/secureTypes' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/properties/testPropertyKey1' \
  -H 'accept: application/json'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/properties/testPropertyKey1' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/properties/testPropertyKey2/testPropertyValue2' \
  -H 'accept: */*' \
  -d ''
curl -X 'HEAD' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/properties/testPropertyKey2' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/typedConfigurations/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration1' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/typedConfigurations/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration2' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '---
dc: "dc1"
hostname: "localhost"
port: 8080'
curl -I \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/typedConfigurations/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration2' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/typedConfigurations/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration2' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/configurations' \
  -H 'accept: application/json'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/load' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"enableSchedulableConfigurationAccessor":true,"configurationAccessorSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}}'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/properties/testKey123/testValue123' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/reload' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/status' \
  -H 'accept: text/plain'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/configurationListeners/propertyKey/testKey123' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/configurationListeners/appbaseServiceConfigurationType/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration1' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/globalConfigurationListeners/com.viveworks.appbase.stacktest.StackTestServer%24SampleConfigurationListener1' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/nonValidatingTypes/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration1' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/propertyDeserializers/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration1' \
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/propertyDeserializers/configurationTypes/testPropertyKey1' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/secureFields/dc/configurationType/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration1' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/securePropertyKeys/testPropertyKey1' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/configurations/secureTypes/com.viveworks.appbase.stacktest.StackTestServer%24SampleAppbaseServiceConfiguration1' \
  -H 'accept: */*'




##############################
### Exception
##############################
echo "Testing appbase-core: Exception REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionStores' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/globalExceptionListeners' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionListenersByExceptionTypeMap' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionStoreFactoryByConfigurationTypeNameMap' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionMessageDetails' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionMessageDetails/timestamps/1705190917316/1706190917316' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionMessageDetails/id/123' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionMessageDetails/throwable/java.io.IOException' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/uniqueExceptionTypeNames' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/status' \
  -H 'accept: text/plain'
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/reload' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionListener/exceptionType' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionStoreFactory/configurationType' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionStores' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/globalExceptionListeners' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionMessageDetails/id/123' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionMessageDetails/timestamp/1706190917316' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionListener/com.viveworks.appbase.core.service.exception.listener.LoggingExceptionListener/exceptionType/java.io.IOException' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionStores/com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/exceptionStoreFactory/com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreFactory/configurationType/com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/globalExceptionListeners/com.viveworks.appbase.core.service.exception.listener.LoggingExceptionListener' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/exceptions/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"defaultExceptionAccessorConfiguration":{"exceptionStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration","enabled":true,"discriminator":"discriminator","stackTraceSerializationMaxLength":1024,"exceptionStoreType":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore"}]},"defaultExpiredMessageCleanerConfiguration":{"enabled":true,"exclusiveRemoveBeforeTimePeriod":1,"exclusiveRemoveBeforeTimePeriodUnit":"HOURS","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1,"timeUnit":"HOURS"}}}'




##############################
### Log
##############################
echo "Testing appbase-core: Log REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logRecords/timestamp/1706190917316/pageSize/10' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logRecords/inclusiveStartTimestamp/1705190917316/exclusiveEndTimestamp/1706190917316/pageSize/10' \
  -H 'accept: application/json'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/readLogRecordsByCriteria/pageSize/10' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"logLevelCriteria": {"logLevel": "INFO","match": "EQUAL"}}'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logStoreFactory/configurationType' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logStores' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/status' \
  -H 'accept: text/plain'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logRecords/timestamp/1706190917316' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logRecords/inclusiveStartTimestamp/1705190917316/exclusiveEndTimestamp/1706190917316' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/reload' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"logbackXml":null,"defaultLogAccessorConfiguration":{"enabled":true,"logStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration","enabled":true,"logStoreType":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore"}]},"metricsTurboFilterHandlerConfiguration":{"enabled":true,"measureLogLevels":true,"measureLoggerNames":true,"allowedLoggerNames":[],"blockedLoggerNames":[],"measureFormattedMessages":false,"allowedFormattedMessages":[],"blockedFormattedMessages":[],"measureThrowableTypeNames":true,"allowedThrowableTypeNames":[],"blockedThrowableTypeNames":[]},"burstTurboLogEventFilterHandlerConfiguration":{"enabled":true,"burstRateThreshold":1000.0,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultLogRateCalculatorConfiguration":{"ewmaRate":"ONE_MINUTE"},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"disruptorMediatorEventDelegatingAppenderConfiguration":{"enabled":true},"luceneLogIndexConfiguration":{"type":"com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration","enabled":true,"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/ea5436b7-7626-4240-a382-b8d5ee06b95c","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"timestampStoredFieldName":"creationTimestamp","timestampLongPointName":"creationTimestampLongPoint","threadNameFieldName":"threadName","loggerNameFieldName":"loggerName","logLevelFieldName":"logLevel","formattedMessageFieldName":"formattedMessage","messageFieldName":"message","throwableTypeNameFieldName":"throwable.typeName","throwableMessageFieldName":"throwable.message","throwableStackTraceIndexEnabled":true,"throwableStackTraceFieldName":"throwable.stackTrace","mdcIndexEnabled":true,"mdcFieldNamePrefix":"mdc.","mdcFieldNameSuffix":"","markerIndexEnabled":true,"markerFieldName":"marker","throwableCauseTypeNameFieldName":"throwable.cause.typeName","throwableCauseMessageFieldName":"throwable.cause.message","throwableCauseStackTraceIndexEnabled":true,"throwableCauseStackTraceFieldName":"throwable.cause.stackTrace","callerStackIndexEnabled":false,"callerStackFieldName":"callerStack"},"luceneAppenderConfiguration":{"enabled":true},"disruptorLogAndTurboLogEventFilterDelegatorConfiguration":{"enabled":true,"acceptLogAfterTurboLogEventListeners":true,"acceptLogAfterLogEventListeners":true},"duplicateLogTurboFilterHandlerConfiguration":{"enabled":false,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"luceneLogAndTurboLogEventFilterListenerConfiguration":{"enabled":false,"indexTurboLog":false,"indexLogEvent":false}}'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logStoreFactory/configurationType' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logStores' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/synchronizeLogStores' \
  -H 'accept: */*' \
  -d ''
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/logStores/com.viveworks.appbase.stacktest.sampletypes.SampleLogStore' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/logs/unregisterLogStoreFactory/logStoreFactoryTypeName/com.viveworks.appbase.stacktest.sampletypes.SampleLogStoreFactory/logStoreConfigurationTypeName/com.viveworks.appbase.stacktest.sampletypes.SampleLogStoreConfiguration' \
  -H 'accept: */*'




##############################
### Metric
##############################
echo "Testing appbase-core: Metric REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/micrometerRegistryNames' \
  -H 'accept: application/json'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/clearMicrometerRegistries' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/unregisterMicrometerRegistry/io.micrometer.core.instrument.logging.LoggingMeterRegistry' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/serializeMetricRegistryAsJson' \
  -H 'accept: application/json'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/reload' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"enableJvmAndSystemMetrics":true,"jmxMeterRegistryConfiguration":{"enabled":false,"additionalProperties":{},"prefix":"jmx","domain":"com.viveworks.appbase.metric","step":"PT1M"}}'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/metrics/status' \
  -H 'accept: text/plain'




##############################
### ServiceRegistry
##############################
echo "Testing appbase-core: ServiceRegistry REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/appbaseServiceStatuses' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/appbaseServiceStatuses/com.viveworks.appbase.core.service.validation.DefaultValidationService' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/overallStatus' \
  -H 'accept: application/json'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/pollAndUpdateStatuses' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/pollOverallStatus' \
  -H 'accept: application/json' \
  -d ''
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/status' \
  -H 'accept: text/plain'
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/reload' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"appName":"Appbase","enablePolling":true,"registerAsConfigurationListener":true,"pollingSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}}'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/appbaseServices/com.viveworks.appbase.core.service.validation.DefaultValidationService' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/serviceRegistries/appbaseServices' \
  -H 'accept: */*'




##############################
### Mediator
##############################
echo "Testing appbase-core: Mediator REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/configurationFilePath' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/isInitWithConfigurationFile' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/isInitWithDefaultConfiguration' \
  -H 'accept: application/json'
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/reload' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}}'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/mediator/status' \
  -H 'accept: text/plain'




##############################
### JMX
##############################
echo "Testing appbase-core: JMX REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/mxBeanEntries' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/mxBeanEntries/com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService' \
  -H 'accept: application/json'
curl -X 'HEAD' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/mxBeans/com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/status' \
  -H 'accept: text/plain'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/reload' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true,"excludedMxBeanRegistrations":[]}'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/mxBeans/com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService' \
  -H 'accept: */*'
curl -X 'DELETE' \
  'http://localhost:2409/appbase-core/restapi/v1/jmx/mxBeans' \
  -H 'accept: */*'




##############################
### Core
##############################
echo "Testing appbase-core: Core REST API"

curl -X 'GET' \
  'http://localhost:2409/appbase-core/restapi/v1/core/status' \
  -H 'accept: text/plain'
curl -X 'PUT' \
  'http://localhost:2409/appbase-core/restapi/v1/core/reload' \
  -H 'accept: */*'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/core/reloadService/com.viveworks.appbase.core.service.mediator.DefaultMediatorService/com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}}'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/core/shutdown' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/core/shutdownAllServices' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/core/shutdownService/com.viveworks.appbase.core.service.mediator.DefaultMediatorService' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/core/reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"defaultDisruptorServiceConfiguration":{"waitStrategyConfiguration":{"type":"com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration","enabled":true,"retries":100,"sleepTimeNanoSeconds":1001000},"eventHandlerCount":1,"ringBufferSize":1024,"producerType":"MULTI","threadFactoryConfiguration":{"nameFormat":"disruptor-thread-%d","daemon":false,"enableThreadAffinity":true}},"defaultLuceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/e0c1f264-7c94-48db-99cb-2ca81d160ac3","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"defaultLogServiceConfiguration":{"logbackXml":null,"defaultLogAccessorConfiguration":{"enabled":true,"logStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration","enabled":true,"logStoreType":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore"}]},"metricsTurboFilterHandlerConfiguration":{"enabled":true,"measureLogLevels":true,"measureLoggerNames":true,"allowedLoggerNames":[],"blockedLoggerNames":[],"measureFormattedMessages":false,"allowedFormattedMessages":[],"blockedFormattedMessages":[],"measureThrowableTypeNames":true,"allowedThrowableTypeNames":[],"blockedThrowableTypeNames":[]},"burstTurboLogEventFilterHandlerConfiguration":{"enabled":true,"burstRateThreshold":1000.0,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultLogRateCalculatorConfiguration":{"ewmaRate":"ONE_MINUTE"},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"disruptorMediatorEventDelegatingAppenderConfiguration":{"enabled":true},"luceneLogIndexConfiguration":{"type":"com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration","enabled":true,"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/ee49fe9a-17bf-4a8d-b8e9-425dc67fb2f4","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"timestampStoredFieldName":"creationTimestamp","timestampLongPointName":"creationTimestampLongPoint","threadNameFieldName":"threadName","loggerNameFieldName":"loggerName","logLevelFieldName":"logLevel","formattedMessageFieldName":"formattedMessage","messageFieldName":"message","throwableTypeNameFieldName":"throwable.typeName","throwableMessageFieldName":"throwable.message","throwableStackTraceIndexEnabled":true,"throwableStackTraceFieldName":"throwable.stackTrace","mdcIndexEnabled":true,"mdcFieldNamePrefix":"mdc.","mdcFieldNameSuffix":"","markerIndexEnabled":true,"markerFieldName":"marker","throwableCauseTypeNameFieldName":"throwable.cause.typeName","throwableCauseMessageFieldName":"throwable.cause.message","throwableCauseStackTraceIndexEnabled":true,"throwableCauseStackTraceFieldName":"throwable.cause.stackTrace","callerStackIndexEnabled":false,"callerStackFieldName":"callerStack"},"luceneAppenderConfiguration":{"enabled":true},"disruptorLogAndTurboLogEventFilterDelegatorConfiguration":{"enabled":true,"acceptLogAfterTurboLogEventListeners":true,"acceptLogAfterLogEventListeners":true},"duplicateLogTurboFilterHandlerConfiguration":{"enabled":false,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"luceneLogAndTurboLogEventFilterListenerConfiguration":{"enabled":false,"indexTurboLog":false,"indexLogEvent":false}},"defaultMetricServiceConfiguration":{"enableJvmAndSystemMetrics":true,"jmxMeterRegistryConfiguration":{"enabled":false,"additionalProperties":{},"prefix":"jmx","domain":"com.viveworks.appbase.metric","step":"PT1M"}},"defaultValidationServiceConfiguration":{},"defaultMediatorServiceConfiguration":{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}},"defaultConfigurationServiceConfiguration":{"enableSchedulableConfigurationAccessor":true,"configurationAccessorSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"defaultServiceRegistryConfiguration":{"appName":"Appbase","enablePolling":true,"registerAsConfigurationListener":true,"pollingSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"defaultExceptionServiceConfiguration":{"defaultExceptionAccessorConfiguration":{"exceptionStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration","enabled":true,"discriminator":"discriminator","stackTraceSerializationMaxLength":1024,"exceptionStoreType":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore"}]},"defaultExpiredMessageCleanerConfiguration":{"enabled":true,"exclusiveRemoveBeforeTimePeriod":1,"exclusiveRemoveBeforeTimePeriodUnit":"HOURS","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1,"timeUnit":"HOURS"}}},"defaultTracerServiceConfiguration":{"serviceName":"AppbaseService","tags":{},"use128BitTraceId":true,"expandExceptionLogs":true,"jaegerPropagationConfiguration":{"jaegerPropagationTypes":["W3C_TRACE_CONTEXT"],"baggagePrefix":"baggage-"},"jaegerSamplerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration","decision":true},"jaegerScopeManagerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration","mdcTraceIdKey":"trace.id","mdcSpanIdKey":"trace.spanId","mdcSampledKey":"trace.sampled"},"jaegerReporterConfigurations":[{"type":"com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration","enabled":true}]},"defaultReactorServiceConfiguration":{"enabled":true,"schedulerConfigurations":[{"type":"com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration","id":"Appbase-Reactor-MediatorExecutorServiceScheduler","defaultScheduler":true,"executorName":"appbase-reactor-scheduler-mediator-executor"}]},"defaultJmxServiceConfiguration":{"enabled":true,"excludedMxBeanRegistrations":[]},"jmxServiceConfiguration":{"enabled":true,"excludedMxBeanRegistrations":[]},"logServiceConfiguration":{"logbackXml":null,"defaultLogAccessorConfiguration":{"enabled":true,"logStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration","enabled":true,"logStoreType":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore"}]},"metricsTurboFilterHandlerConfiguration":{"enabled":true,"measureLogLevels":true,"measureLoggerNames":true,"allowedLoggerNames":[],"blockedLoggerNames":[],"measureFormattedMessages":false,"allowedFormattedMessages":[],"blockedFormattedMessages":[],"measureThrowableTypeNames":true,"allowedThrowableTypeNames":[],"blockedThrowableTypeNames":[]},"burstTurboLogEventFilterHandlerConfiguration":{"enabled":true,"burstRateThreshold":1000.0,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultLogRateCalculatorConfiguration":{"ewmaRate":"ONE_MINUTE"},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"disruptorMediatorEventDelegatingAppenderConfiguration":{"enabled":true},"luceneLogIndexConfiguration":{"type":"com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration","enabled":true,"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/ee49fe9a-17bf-4a8d-b8e9-425dc67fb2f4","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"timestampStoredFieldName":"creationTimestamp","timestampLongPointName":"creationTimestampLongPoint","threadNameFieldName":"threadName","loggerNameFieldName":"loggerName","logLevelFieldName":"logLevel","formattedMessageFieldName":"formattedMessage","messageFieldName":"message","throwableTypeNameFieldName":"throwable.typeName","throwableMessageFieldName":"throwable.message","throwableStackTraceIndexEnabled":true,"throwableStackTraceFieldName":"throwable.stackTrace","mdcIndexEnabled":true,"mdcFieldNamePrefix":"mdc.","mdcFieldNameSuffix":"","markerIndexEnabled":true,"markerFieldName":"marker","throwableCauseTypeNameFieldName":"throwable.cause.typeName","throwableCauseMessageFieldName":"throwable.cause.message","throwableCauseStackTraceIndexEnabled":true,"throwableCauseStackTraceFieldName":"throwable.cause.stackTrace","callerStackIndexEnabled":false,"callerStackFieldName":"callerStack"},"luceneAppenderConfiguration":{"enabled":true},"disruptorLogAndTurboLogEventFilterDelegatorConfiguration":{"enabled":true,"acceptLogAfterTurboLogEventListeners":true,"acceptLogAfterLogEventListeners":true},"duplicateLogTurboFilterHandlerConfiguration":{"enabled":false,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"luceneLogAndTurboLogEventFilterListenerConfiguration":{"enabled":false,"indexTurboLog":false,"indexLogEvent":false}},"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/e0c1f264-7c94-48db-99cb-2ca81d160ac3","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"reactorServiceConfiguration":{"enabled":true,"schedulerConfigurations":[{"type":"com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration","id":"Appbase-Reactor-MediatorExecutorServiceScheduler","defaultScheduler":true,"executorName":"appbase-reactor-scheduler-mediator-executor"}]},"tracerServiceConfiguration":{"serviceName":"AppbaseService","tags":{},"use128BitTraceId":true,"expandExceptionLogs":true,"jaegerPropagationConfiguration":{"jaegerPropagationTypes":["W3C_TRACE_CONTEXT"],"baggagePrefix":"baggage-"},"jaegerSamplerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration","decision":true},"jaegerScopeManagerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration","mdcTraceIdKey":"trace.id","mdcSpanIdKey":"trace.spanId","mdcSampledKey":"trace.sampled"},"jaegerReporterConfigurations":[{"type":"com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration","enabled":true}]},"exceptionServiceConfiguration":{"defaultExceptionAccessorConfiguration":{"exceptionStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration","enabled":true,"discriminator":"discriminator","stackTraceSerializationMaxLength":1024,"exceptionStoreType":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore"}]},"defaultExpiredMessageCleanerConfiguration":{"enabled":true,"exclusiveRemoveBeforeTimePeriod":1,"exclusiveRemoveBeforeTimePeriodUnit":"HOURS","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1,"timeUnit":"HOURS"}}},"serviceRegistryConfiguration":{"appName":"Appbase","enablePolling":true,"registerAsConfigurationListener":true,"pollingSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"configurationServiceConfiguration":{"enableSchedulableConfigurationAccessor":true,"configurationAccessorSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"mediatorServiceConfiguration":{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}},"validationServiceConfiguration":{},"metricServiceConfiguration":{"enableJvmAndSystemMetrics":true,"jmxMeterRegistryConfiguration":{"enabled":false,"additionalProperties":{},"prefix":"jmx","domain":"com.viveworks.appbase.metric","step":"PT1M"}},"disruptorServiceConfiguration":{"waitStrategyConfiguration":{"type":"com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration","enabled":true,"retries":100,"sleepTimeNanoSeconds":1001000},"eventHandlerCount":1,"ringBufferSize":1024,"producerType":"MULTI","threadFactoryConfiguration":{"nameFormat":"disruptor-thread-%d","daemon":false,"enableThreadAffinity":true}}}'




##############################
### Status
##############################
echo "Testing appbase-core: Status REST API"

curl -X 'HEAD' \
  'http://localhost:2409/appbase-core/restapi/v1/status/overallStatus' \
  -H 'accept: application/json'
curl -X 'POST' \
  'http://localhost:2409/appbase-core/restapi/v1/status/pollOverallStatus' \
  -H 'accept: application/json' \
  -d ''

