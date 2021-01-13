# Logstash PDF Filter Plugin

This plugin will read pdf data and add metadata and extract the content to plain text from the binary

### Properties

| Properties  | Mandatory | Default | Description |
| ------------- | ------------- | ------------- | ------------- |
| metadataCustom  | false  | NA  | Map of custom metadata |

#### metadataCustom

´´´
metadataCustom => {
  "MY_CUSTOM_METADATA" => "METADATA_VALUE"
}
´´´´