# Logstash Confluence Input Plugin

This Plugin reads confluence spaces and 

- Extracts user and groups
- Extracts Sites / Pages and Attachments

The data is sent as a byte[] and all headers are added to the hashmap send to elastic. 
A tracking mecanism is built using an elasticsearch instance. 
No processing of the data is done so it can be processed , as is, by the logstash filters.

### Properties

| Properties  | Mandatory | Default | Description |
| ------------- | ------------- | ------------- | ------------- |
| url  | true  | NA  | Confluence url |
| username  | true  | NA  | Confluence Username |
| password  | true  | NA  | Confluence Password |
| dataFolder  | true  | NA  | Where to store the data folder, its a local queue |
| userSyncCron  | true  | NA  | Cron when to start the user sync |
| dataSyncCron  | true  | NA  | Cron when to start the data sync |
| enableUserSync  | false  | true  | Enable sync all Users |
| userSyncBatchSize  | false  | 1000  | User Batch Size |
| userSyncThreadSize  | false  | 1000  | User Thread Size |
| enableDataSync  | false  | true  | Enable sync confluence data |
| dataSyncBatchSize  | false  | 100  | Data Batch Size |
| dataSyncThreadSize  | false  | 3  | Data Thread Size |
| spaces  | false  | All Spaces  | Which spaces need to be crawled |
| pageLimit  | false  | 25  | Query page limit |
| sleep  | false  | 1  | Sleep between calls |
| dataAttachmentsInclude  | false  | empty array  | Array of regexes that must match attachments to index |
| dataAttachmentsExclude  | false  | empty array  | Array of regexes that must match attachments to not index |
| dataAttachmentsInclude  | false  | empty array  | Array of regexes that must match attachments to index |
| dataPageExclude  | false  | empty array  | Array of regexes that must match pages to not index |
| dataSpaceExclude  | false  | empty array  | Array of regexes that must match spaces to not index |
| dataAttachmentsMaxSize  | false  | NA  | Max file size |