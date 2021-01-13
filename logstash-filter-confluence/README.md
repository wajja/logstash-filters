# Logstash Confluence Filter Plugin

This plugin will read Confluence data and add metadata

### Properties

| Properties  | Mandatory | Default | Description |
| ------------- | ------------- | ------------- | ------------- |
| metadataCustom  | false  | NA  | Map of custom metadata |
| simplifiedContentType  | false  | NA  | Map of custom content extensions to readable values |

#### metadataCustom

´´´
metadataCustom => {
  "MY_CUSTOM_METADATA" => "METADATA_VALUE"
}
´´´´

#### simplifiedContentType

´´´
simplifiedContentType => {
  "ppt" => "Powerpoint"
}
´´´´