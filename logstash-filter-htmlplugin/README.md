# Logstash HTML Filter Plugin

This plugin will read html data and add metadata

### Properties

| Properties  | Mandatory | Default | Description |
| ------------- | ------------- | ------------- | ------------- |
| metadataCustom  | false  | NA  | Map of custom metadata |
| metadataMapping  | false  | NA  | Map of <meta> fields that should map to another value |
| metadataMapping  | false  | NA  | Array of content to remove from the main body |
| extractTitleCss  | false  | NA  | Array of CSS where you can find the title of the page |
| extractBodyCss  | false  | NA  | Array of CSS where you can find the body of the page |
| excludeBodyCss  | false  | NA  | Array of CSS that needs to be remove from the content |
	
#### metadataCustom

´´´
metadataCustom => {
  "MY_CUSTOM_METADATA" => "METADATA_VALUE"
}
´´´´