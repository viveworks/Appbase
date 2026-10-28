echo 'Running appbase-stacktest: appbase-core servlets curl tests...'


##############################
### Configuration
##############################
echo "Testing appbase-core: Configuration servlet"

curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearConfigurationListeners'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearGlobalConfigurationListeners'
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=nonValidatingTypes'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearNonValidatingTypes'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearPropertyDeserializerAndConfigurationTypesMap'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearPropertyDeserializers'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearSecureFieldByConfigurationTypeMap'
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=securePropertyKeys'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearSecurePropertyKeys'
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=secureTypes'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=clearSecureTypes'
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=property&propertyKey=testPropertyKey1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=property&propertyKey=testPropertyKey1'
curl -v -X 'POST' \
  'http://localhost:2409/configuration?operation=property&propertyKey=testPropertyKey2&propertyValue=testValue2'
curl -v -I \
  'http://localhost:2409/configuration?operation=property&propertyKey=testPropertyKey2'
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=typedConfiguration&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration1'
curl -v -X 'POST' \
  'http://localhost:2409/configuration?operation=typedConfiguration&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration2' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '---
dc: "dc1"
hostname: "localhost"
port: 8080'
curl -I \
  'http://localhost:2409/configuration?operation=typedConfiguration&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration2'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=typedConfiguration&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration2'
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=configurationsAsJson'
curl -v -X 'POST' \
  'http://localhost:2409/configuration?operation=load' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'POST' \
  'http://localhost:2409/configuration?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"enableSchedulableConfigurationAccessor":true,"configurationAccessorSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}}'
curl -X 'POST' \
  'http://localhost:2409/configuration?operation=property&propertyKey=testKey123&propertyValue=testValue123' \
  -H 'accept: */*' \
  -d ''
curl -X 'POST' \
  'http://localhost:2409/configuration?operation=shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'PUT' \
  'http://localhost:2409/configuration?operation=reload' \
  -H 'accept: */*' \
  -d ''
curl -X 'GET' \
  'http://localhost:2409/configuration?operation=status' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterConfigurationListenerByPropertyKey&propertyKey=testPropertyKey1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterConfigurationListenerByTypeName&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterGlobalConfigurationListener&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleConfigurationListener1'
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterNonValidatingType&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterPropertyDeserializer&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterPropertyDeserializerAndConfigurationTypesByPropertyKey&propertyKey=testPropertyKey1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterSecureFieldByConfigurationType&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration1&fieldName=dc'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterSecurePropertyKey&propertyKey=testPropertyKey1'
curl -v -X 'DELETE' \
  'http://localhost:2409/configuration?operation=unregisterSecureType&typeName=com.viveworks.appbase.stacktest.StackTestServer$SampleAppbaseServiceConfiguration1'




##############################
### Exception
##############################
echo "Testing appbase-core: Exception servlet"

curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionStores' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=globalExceptionListeners' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionListenersByExceptionTypeMap' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionStoreFactoryByConfigurationTypeNameMap' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionMessageDetails' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionMessageDetailsByTimestamps&inclusiveMinTimestamp=1705190917316&exclusiveMaxTimestamp=1706190917316' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionMessageDetailsById&id=123' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=exceptionMessageDetailsByThrowableTypeName&throwableTypeName=java.io.IOException' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=uniqueExceptionTypeNames' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/exception?operation=status' \
  -H 'accept: application/json'
curl -v -X 'PUT' \
  'http://localhost:2409/exception?operation=reload' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=clearExceptionListenerByExceptionTypeMap'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=clearExceptionStoreFactoryByConfigurationTypeMap'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=clearExceptionStores'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=clearGlobalExceptionListeners'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=removeExceptionMessageDetailsById&id=123'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=removeExceptionMessageDetailsExclusiveBefore&timestamp=1704380912'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=unregisterExceptionListenerByExceptionType&exceptionTypeName=java.io.IOException&exceptionListenerTypeName=com.viveworks.appbase.core.service.exception.listener.LoggingExceptionListener'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=unregisterExceptionStore&exceptionStoreTypeName=com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=unregisterExceptionStoreFactoryByConfigurationType&exceptionStoreConfigurationTypeName=com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration&exceptionStoreFactoryTypeName=com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreFactory'
curl -v -X 'DELETE' \
  'http://localhost:2409/exception?operation=unregisterGlobalExceptionListener&exceptionListenerTypeName=com.viveworks.appbase.core.service.exception.listener.LoggingExceptionListener'
curl -X 'POST' \
  'http://localhost:2409/exception?operation=shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'POST' \
  'http://localhost:2409/exception?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"defaultExceptionAccessorConfiguration":{"exceptionStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration","enabled":true,"discriminator":"discriminator","stackTraceSerializationMaxLength":1024,"exceptionStoreType":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore"}]},"defaultExpiredMessageCleanerConfiguration":{"enabled":true,"exclusiveRemoveBeforeTimePeriod":1,"exclusiveRemoveBeforeTimePeriodUnit":"HOURS","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1,"timeUnit":"HOURS"}}}'




##############################
### Log
##############################
echo "Testing appbase-core: Log servlet"

curl -X 'GET' \
  'http://localhost:2409/log?operation=logRecordsAt&timestamp=1706190917316&pageSize=10' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/log?operation=logRecordsBetween&inclusiveStartTimestamp=1705190917316&exclusiveEndTimestamp=1706190917316&pageSize=10' \
  -H 'accept: application/json'
curl -v -X 'POST' \
  'http://localhost:2409/log?operation=readLogRecordsByCriteria&pageSize=10' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"logLevelCriteria": {"logLevel": "INFO","match": "EQUAL"}}'
curl -X 'GET' \
  'http://localhost:2409/log?operation=logStoreFactoryByConfigurationTypeMap' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/log?operation=logStores' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/log?operation=status' \
  -H 'accept: text/plain'
curl -v -X 'DELETE' \
  'http://localhost:2409/log?operation=removeLogRecordsAt&timestamp=1705190917316' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/log?operation=removeLogRecordsBetween&inclusiveStartTimestamp=1705190917316&exclusiveEndTimestamp=1706190917316' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/log?operation=shutdown' \
  -H 'accept: */*'
  -d ''
curl -v -X 'PUT' \
  'http://localhost:2409/log?operation=reload' \
  -H 'accept: application/json'
curl -v -X 'POST' \
  'http://localhost:2409/log?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"logbackXml":null,"defaultLogAccessorConfiguration":{"enabled":true,"logStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration","enabled":true,"logStoreType":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore"}]},"metricsTurboFilterHandlerConfiguration":{"enabled":true,"measureLogLevels":true,"measureLoggerNames":true,"allowedLoggerNames":[],"blockedLoggerNames":[],"measureFormattedMessages":false,"allowedFormattedMessages":[],"blockedFormattedMessages":[],"measureThrowableTypeNames":true,"allowedThrowableTypeNames":[],"blockedThrowableTypeNames":[]},"burstTurboLogEventFilterHandlerConfiguration":{"enabled":true,"burstRateThreshold":1000.0,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultLogRateCalculatorConfiguration":{"ewmaRate":"ONE_MINUTE"},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"disruptorMediatorEventDelegatingAppenderConfiguration":{"enabled":true},"luceneLogIndexConfiguration":{"type":"com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration","enabled":true,"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/ea5436b7-7626-4240-a382-b8d5ee06b95c","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"timestampStoredFieldName":"creationTimestamp","timestampLongPointName":"creationTimestampLongPoint","threadNameFieldName":"threadName","loggerNameFieldName":"loggerName","logLevelFieldName":"logLevel","formattedMessageFieldName":"formattedMessage","messageFieldName":"message","throwableTypeNameFieldName":"throwable.typeName","throwableMessageFieldName":"throwable.message","throwableStackTraceIndexEnabled":true,"throwableStackTraceFieldName":"throwable.stackTrace","mdcIndexEnabled":true,"mdcFieldNamePrefix":"mdc.","mdcFieldNameSuffix":"","markerIndexEnabled":true,"markerFieldName":"marker","throwableCauseTypeNameFieldName":"throwable.cause.typeName","throwableCauseMessageFieldName":"throwable.cause.message","throwableCauseStackTraceIndexEnabled":true,"throwableCauseStackTraceFieldName":"throwable.cause.stackTrace","callerStackIndexEnabled":false,"callerStackFieldName":"callerStack"},"luceneAppenderConfiguration":{"enabled":true},"disruptorLogAndTurboLogEventFilterDelegatorConfiguration":{"enabled":true,"acceptLogAfterTurboLogEventListeners":true,"acceptLogAfterLogEventListeners":true},"duplicateLogTurboFilterHandlerConfiguration":{"enabled":false,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"luceneLogAndTurboLogEventFilterListenerConfiguration":{"enabled":false,"indexTurboLog":false,"indexLogEvent":false}}'
curl -v -X 'DELETE' \
  'http://localhost:2409/log?operation=clearLogStoreFactoryByConfigurationTypeMap' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/log?operation=clearLogStores' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/log?operation=synchronizeLogStores' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/log?operation=unregisterLogStore&logStoreTypeName=com.viveworks.appbase.stacktest.sampletypes.SampleLogStore' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/log?operation=unregisterLogStoreFactoryByConfigurationType&logStoreFactoryTypeName=com.viveworks.appbase.stacktest.sampletypes.SampleLogStoreFactory&logStoreConfigurationTypeName=com.viveworks.appbase.stacktest.sampletypes.SampleLogStoreConfiguration' \
  -H 'accept: */*'




##############################
### Metric
##############################
echo "Testing appbase-core: Metric servlet"

curl -X 'GET' \
  'http://localhost:2409/metric?operation=micrometerRegistryNames' \
  -H 'accept: application/json'
curl -v -X 'DELETE' \
  'http://localhost:2409/metric?operation=clearMicrometerRegistries' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/metric?operation=unregisterMicrometerRegistry&meterRegistryTypeName=io.micrometer.core.instrument.logging.LoggingMeterRegistry' \
  -H 'accept: */*'
curl -X 'GET' \
  'http://localhost:2409/metric?operation=serializeMetricRegistryAsJson' \
  -H 'accept: application/json'
curl -v -X 'POST' \
  'http://localhost:2409/metric?operation=shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'PUT' \
  'http://localhost:2409/metric?operation=reload' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/metric?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"enableJvmAndSystemMetrics":true,"jmxMeterRegistryConfiguration":{"enabled":false,"additionalProperties":{},"prefix":"jmx","domain":"com.viveworks.appbase.metric","step":"PT1M"}}'
curl -X 'GET' \
  'http://localhost:2409/metric?operation=status' \
  -H 'accept: text/plain'




##############################
### ServiceRegistry
##############################
echo "Testing appbase-core: ServiceRegistry servlet"

curl -X 'GET' \
  'http://localhost:2409/serviceRegistry?operation=appbaseServiceStatuses' \
  -H 'accept: application/json'
curl -v -X 'GET' \
  'http://localhost:2409/serviceRegistry?operation=appbaseServiceStatus&appbaseServiceTypeName=com.viveworks.appbase.core.service.validation.DefaultValidationService' \
  -H 'accept: application/json'
curl -v -X 'GET' \
  'http://localhost:2409/serviceRegistry?operation=overallStatus' \
  -H 'accept: application/json'
curl -v -X 'POST' \
  'http://localhost:2409/serviceRegistry?operation=pollAndUpdateStatuses' \
  -H 'accept: application/json'
curl -v -X 'POST' \
  'http://localhost:2409/serviceRegistry?operation=pollOverallStatus' \
  -H 'accept: application/json' \
  -d ''
curl -v -X 'GET' \
  'http://localhost:2409/serviceRegistry?operation=status' \
  -H 'accept: text/plain'
curl -v -X 'PUT' \
  'http://localhost:2409/serviceRegistry?operation=reload' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/serviceRegistry?operation=shutdown' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/serviceRegistry?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"appName":"Appbase","enablePolling":true,"registerAsConfigurationListener":true,"pollingSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}}'
curl -v -X 'DELETE' \
  'http://localhost:2409/serviceRegistry?operation=unregisterAppbaseService&appbaseServiceTypeName=com.viveworks.appbase.core.service.validation.DefaultValidationService' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/serviceRegistry?operation=clearAppbaseServices' \
  -H 'accept: */*'




##############################
### Mediator
##############################
echo "Testing appbase-core: Mediator servlet"

curl -X 'GET' \
  'http://localhost:2409/mediator?operation=configurationFilePath' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/mediator?operation=isInitWithConfigurationFile' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/mediator?operation=isInitWithDefaultConfiguration' \
  -H 'accept: application/json'
curl -v -X 'PUT' \
  'http://localhost:2409/mediator?operation=reload' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/mediator?operation=shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'POST' \
  'http://localhost:2409/mediator?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}}'
curl -X 'GET' \
  'http://localhost:2409/mediator?operation=status' \
  -H 'accept: text/plain'




##############################
### JMX
##############################
echo "Testing appbase-core: JMX servlet"

curl -X 'GET' \
  'http://localhost:2409/jmx?operation=mxBeanEntries' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/jmx?operation=mxBeanEntry&mxBeanTypeName=com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService' \
  -H 'accept: application/json'
curl -I \
  'http://localhost:2409/jmx?operation=isMXBeanRegistered&mxBeanTypeName=com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService' \
  -H 'accept: application/json'
curl -X 'GET' \
  'http://localhost:2409/jmx?operation=status' \
  -H 'accept: text/plain'
curl -v -X 'POST' \
  'http://localhost:2409/jmx?operation=shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'PUT' \
  'http://localhost:2409/jmx?operation=reload' \
  -H 'accept: */*'
curl -v -X 'POST' \
  'http://localhost:2409/jmx?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true,"excludedMxBeanRegistrations":[]}'
curl -v -X 'DELETE' \
  'http://localhost:2409/jmx?operation=unregisterMXBean&mxBeanTypeName=com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService' \
  -H 'accept: */*'
curl -v -X 'DELETE' \
  'http://localhost:2409/jmx?operation=clearMXBeans' \
  -H 'accept: */*'




##############################
### Core
##############################
echo "Testing appbase-core: Core servlet"

curl -X 'GET' \
  'http://localhost:2409/core?operation=status' \
  -H 'accept: text/plain'
curl -v -X 'PUT' \
  'http://localhost:2409/core?operation=reload' \
  -H 'accept: text/plain'
curl -v -X 'POST' \
  'http://localhost:2409/core?operation=reloadService&appbaseServiceTypeName=com.viveworks.appbase.core.service.mediator.DefaultMediatorService&appbaseServiceConfigurationTypeName=com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}}'
curl -v -X 'POST' \
  'http://localhost:2409/core?operation=shutdownService&appbaseServiceTypeName=com.viveworks.appbase.core.service.mediator.DefaultMediatorService' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'POST' \
  'http://localhost:2409/core?operation=shutdown' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'POST' \
  'http://localhost:2409/core?operation=shutdownAllServices' \
  -H 'accept: */*' \
  -d ''
curl -v -X 'POST' \
  'http://localhost:2409/core?operation=reload' \
  -H 'accept: */*' \
  -H 'Content-Type: */*' \
  -d '{"defaultDisruptorServiceConfiguration":{"waitStrategyConfiguration":{"type":"com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration","enabled":true,"retries":100,"sleepTimeNanoSeconds":1001000},"eventHandlerCount":1,"ringBufferSize":1024,"producerType":"MULTI","threadFactoryConfiguration":{"nameFormat":"disruptor-thread-%d","daemon":false,"enableThreadAffinity":true}},"defaultLuceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/e0c1f264-7c94-48db-99cb-2ca81d160ac3","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"defaultLogServiceConfiguration":{"logbackXml":null,"defaultLogAccessorConfiguration":{"enabled":true,"logStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration","enabled":true,"logStoreType":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore"}]},"metricsTurboFilterHandlerConfiguration":{"enabled":true,"measureLogLevels":true,"measureLoggerNames":true,"allowedLoggerNames":[],"blockedLoggerNames":[],"measureFormattedMessages":false,"allowedFormattedMessages":[],"blockedFormattedMessages":[],"measureThrowableTypeNames":true,"allowedThrowableTypeNames":[],"blockedThrowableTypeNames":[]},"burstTurboLogEventFilterHandlerConfiguration":{"enabled":true,"burstRateThreshold":1000.0,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultLogRateCalculatorConfiguration":{"ewmaRate":"ONE_MINUTE"},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"disruptorMediatorEventDelegatingAppenderConfiguration":{"enabled":true},"luceneLogIndexConfiguration":{"type":"com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration","enabled":true,"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/ee49fe9a-17bf-4a8d-b8e9-425dc67fb2f4","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"timestampStoredFieldName":"creationTimestamp","timestampLongPointName":"creationTimestampLongPoint","threadNameFieldName":"threadName","loggerNameFieldName":"loggerName","logLevelFieldName":"logLevel","formattedMessageFieldName":"formattedMessage","messageFieldName":"message","throwableTypeNameFieldName":"throwable.typeName","throwableMessageFieldName":"throwable.message","throwableStackTraceIndexEnabled":true,"throwableStackTraceFieldName":"throwable.stackTrace","mdcIndexEnabled":true,"mdcFieldNamePrefix":"mdc.","mdcFieldNameSuffix":"","markerIndexEnabled":true,"markerFieldName":"marker","throwableCauseTypeNameFieldName":"throwable.cause.typeName","throwableCauseMessageFieldName":"throwable.cause.message","throwableCauseStackTraceIndexEnabled":true,"throwableCauseStackTraceFieldName":"throwable.cause.stackTrace","callerStackIndexEnabled":false,"callerStackFieldName":"callerStack"},"luceneAppenderConfiguration":{"enabled":true},"disruptorLogAndTurboLogEventFilterDelegatorConfiguration":{"enabled":true,"acceptLogAfterTurboLogEventListeners":true,"acceptLogAfterLogEventListeners":true},"duplicateLogTurboFilterHandlerConfiguration":{"enabled":false,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"luceneLogAndTurboLogEventFilterListenerConfiguration":{"enabled":false,"indexTurboLog":false,"indexLogEvent":false}},"defaultMetricServiceConfiguration":{"enableJvmAndSystemMetrics":true,"jmxMeterRegistryConfiguration":{"enabled":false,"additionalProperties":{},"prefix":"jmx","domain":"com.viveworks.appbase.metric","step":"PT1M"}},"defaultValidationServiceConfiguration":{},"defaultMediatorServiceConfiguration":{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}},"defaultConfigurationServiceConfiguration":{"enableSchedulableConfigurationAccessor":true,"configurationAccessorSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"defaultServiceRegistryConfiguration":{"appName":"Appbase","enablePolling":true,"registerAsConfigurationListener":true,"pollingSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"defaultExceptionServiceConfiguration":{"defaultExceptionAccessorConfiguration":{"exceptionStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration","enabled":true,"discriminator":"discriminator","stackTraceSerializationMaxLength":1024,"exceptionStoreType":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore"}]},"defaultExpiredMessageCleanerConfiguration":{"enabled":true,"exclusiveRemoveBeforeTimePeriod":1,"exclusiveRemoveBeforeTimePeriodUnit":"HOURS","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1,"timeUnit":"HOURS"}}},"defaultTracerServiceConfiguration":{"serviceName":"AppbaseService","tags":{},"use128BitTraceId":true,"expandExceptionLogs":true,"jaegerPropagationConfiguration":{"jaegerPropagationTypes":["W3C_TRACE_CONTEXT"],"baggagePrefix":"baggage-"},"jaegerSamplerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration","decision":true},"jaegerScopeManagerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration","mdcTraceIdKey":"trace.id","mdcSpanIdKey":"trace.spanId","mdcSampledKey":"trace.sampled"},"jaegerReporterConfigurations":[{"type":"com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration","enabled":true}]},"defaultReactorServiceConfiguration":{"enabled":true,"schedulerConfigurations":[{"type":"com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration","id":"Appbase-Reactor-MediatorExecutorServiceScheduler","defaultScheduler":true,"executorName":"appbase-reactor-scheduler-mediator-executor"}]},"defaultJmxServiceConfiguration":{"enabled":true,"excludedMxBeanRegistrations":[]},"jmxServiceConfiguration":{"enabled":true,"excludedMxBeanRegistrations":[]},"logServiceConfiguration":{"logbackXml":null,"defaultLogAccessorConfiguration":{"enabled":true,"logStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration","enabled":true,"logStoreType":"com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore"}]},"metricsTurboFilterHandlerConfiguration":{"enabled":true,"measureLogLevels":true,"measureLoggerNames":true,"allowedLoggerNames":[],"blockedLoggerNames":[],"measureFormattedMessages":false,"allowedFormattedMessages":[],"blockedFormattedMessages":[],"measureThrowableTypeNames":true,"allowedThrowableTypeNames":[],"blockedThrowableTypeNames":[]},"burstTurboLogEventFilterHandlerConfiguration":{"enabled":true,"burstRateThreshold":1000.0,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultLogRateCalculatorConfiguration":{"ewmaRate":"ONE_MINUTE"},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"disruptorMediatorEventDelegatingAppenderConfiguration":{"enabled":true},"luceneLogIndexConfiguration":{"type":"com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration","enabled":true,"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/ee49fe9a-17bf-4a8d-b8e9-425dc67fb2f4","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"timestampStoredFieldName":"creationTimestamp","timestampLongPointName":"creationTimestampLongPoint","threadNameFieldName":"threadName","loggerNameFieldName":"loggerName","logLevelFieldName":"logLevel","formattedMessageFieldName":"formattedMessage","messageFieldName":"message","throwableTypeNameFieldName":"throwable.typeName","throwableMessageFieldName":"throwable.message","throwableStackTraceIndexEnabled":true,"throwableStackTraceFieldName":"throwable.stackTrace","mdcIndexEnabled":true,"mdcFieldNamePrefix":"mdc.","mdcFieldNameSuffix":"","markerIndexEnabled":true,"markerFieldName":"marker","throwableCauseTypeNameFieldName":"throwable.cause.typeName","throwableCauseMessageFieldName":"throwable.cause.message","throwableCauseStackTraceIndexEnabled":true,"throwableCauseStackTraceFieldName":"throwable.cause.stackTrace","callerStackIndexEnabled":false,"callerStackFieldName":"callerStack"},"luceneAppenderConfiguration":{"enabled":true},"disruptorLogAndTurboLogEventFilterDelegatorConfiguration":{"enabled":true,"acceptLogAfterTurboLogEventListeners":true,"acceptLogAfterLogEventListeners":true},"duplicateLogTurboFilterHandlerConfiguration":{"enabled":false,"defaultLogDeduplicatorConfiguration":{"duplicateLogHandlerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":60,"timeUnit":"SECONDS"}},"defaultDuplicateLogHandlerConfiguration":{"markerName":"DefaultDuplicateLogHandler","formattedMessageLogLevel":"INFO","throwableLogLevel":"ERROR","countTextPrefix":" (count=","countTextSuffix":")","throwableTypeNameAndMessageSeparator":" - ","logParamsAndThrowable":true}},"luceneLogAndTurboLogEventFilterListenerConfiguration":{"enabled":false,"indexTurboLog":false,"indexLogEvent":false}},"luceneServiceConfiguration":{"type":"com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration","enabled":true,"indexBaseDirectory":"/var/folders/dy/xxdl3h1j5kx482tzg6xzss440000gn/T//DefaultLuceneService/e0c1f264-7c94-48db-99cb-2ca81d160ac3","directoryType":"NIO","mmapDirectoryConfiguration":{"maxChunkSize":1073741824,"useUnmap":true,"preload":true},"deleteIndexPathUponShutdown":true,"checkPendingFlushUpdate":true,"commitOnClose":true,"maxBufferedDocs":1024,"openMode":"CREATE_OR_APPEND","ramBufferSizeMB":512.0,"ramPerThreadHardLimitMB":128,"readerPooling":true,"useCompoundFile":true,"lruQueryCacheConfiguration":{"enabled":true,"maxQueries":1000,"maxRamBytesUsed":52428800},"standardAnalyzerStopWords":[],"standardAnalyzerMaxTokenLength":255,"stopWordsIgnoreCase":true,"timestampBasedIndexCleanerConfiguration":{"enabled":true,"removeBeforeTimePeriod":1,"removeBeforeTimePeriodUnit":"DAYS","creationTimestampFieldName":"creationTimestampLongPoint","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":10,"timeUnit":"MINUTES"}},"indexSynchronizerSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":30,"timeUnit":"SECONDS"}},"reactorServiceConfiguration":{"enabled":true,"schedulerConfigurations":[{"type":"com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration","id":"Appbase-Reactor-MediatorExecutorServiceScheduler","defaultScheduler":true,"executorName":"appbase-reactor-scheduler-mediator-executor"}]},"tracerServiceConfiguration":{"serviceName":"AppbaseService","tags":{},"use128BitTraceId":true,"expandExceptionLogs":true,"jaegerPropagationConfiguration":{"jaegerPropagationTypes":["W3C_TRACE_CONTEXT"],"baggagePrefix":"baggage-"},"jaegerSamplerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration","decision":true},"jaegerScopeManagerConfiguration":{"type":"com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration","mdcTraceIdKey":"trace.id","mdcSpanIdKey":"trace.spanId","mdcSampledKey":"trace.sampled"},"jaegerReporterConfigurations":[{"type":"com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration","enabled":true}]},"exceptionServiceConfiguration":{"defaultExceptionAccessorConfiguration":{"exceptionStoreConfigurations":[{"type":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration","enabled":true,"discriminator":"discriminator","stackTraceSerializationMaxLength":1024,"exceptionStoreType":"com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStore"}]},"defaultExpiredMessageCleanerConfiguration":{"enabled":true,"exclusiveRemoveBeforeTimePeriod":1,"exclusiveRemoveBeforeTimePeriodUnit":"HOURS","schedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1,"timeUnit":"HOURS"}}},"serviceRegistryConfiguration":{"appName":"Appbase","enablePolling":true,"registerAsConfigurationListener":true,"pollingSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"configurationServiceConfiguration":{"enableSchedulableConfigurationAccessor":true,"configurationAccessorSchedulableConfiguration":{"enabled":true,"initialDelay":0,"interval":1000,"timeUnit":"MILLISECONDS"}},"mediatorServiceConfiguration":{"ignoredMessageTypeSet":[],"messageObjectPoolCapacity":256,"removedMessageQueueCapacity":128,"appbaseRuntimeExceptionQueueCapacity":128,"defaultMessageQueuesConfiguration":{"messageQueueType":"BOUND","boundMessageQueueCapacity":256,"removeExpiredMessages":true,"expiredMessageThresholdMillis":300000,"messageQueuesCleanerInitialDelayMillis":5000,"messageQueuesCleanerIntervalMillis":10000},"sharedExecutorServiceConfiguration":{"executorServiceType":"FIXED","fixedThreadPoolSize":30,"threadFactoryConfiguration":{"nameFormat":"appbase-mediator-thread-%d","daemon":false,"enableThreadAffinity":true}},"sharedScheduledExecutorServiceConfiguration":{"scheduledExecutorServiceType":"SINGLE_THREADED","threadPoolSize":8,"threadFactoryConfiguration":{"nameFormat":"appbase-scheduled-executor-%d","daemon":false,"enableThreadAffinity":true}}},"validationServiceConfiguration":{},"metricServiceConfiguration":{"enableJvmAndSystemMetrics":true,"jmxMeterRegistryConfiguration":{"enabled":false,"additionalProperties":{},"prefix":"jmx","domain":"com.viveworks.appbase.metric","step":"PT1M"}},"disruptorServiceConfiguration":{"waitStrategyConfiguration":{"type":"com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration","enabled":true,"retries":100,"sleepTimeNanoSeconds":1001000},"eventHandlerCount":1,"ringBufferSize":1024,"producerType":"MULTI","threadFactoryConfiguration":{"nameFormat":"disruptor-thread-%d","daemon":false,"enableThreadAffinity":true}}}'




##############################
### Status
##############################
echo "Testing appbase-core: Status servlet"

curl -X 'HEAD' \
  'http://localhost:2409/status?operation=overallStatus' \
  -H 'accept: application/json'
curl -X 'POST' \
  'http://localhost:2409/status?operation=pollOverallStatus' \
  -H 'accept: application/json' \
  -d ''
