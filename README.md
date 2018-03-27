# TreeML
Micro ML parser with validation and expression language.

## Comparison with JSON

| JSON | TreeML |
|---|---|
| Number, Boolean, String, Array, Object | Integer, Double, Boolean, Token, String, List |
| Values can be objects | Values and document structure are cleanly separated |
| Objects are maps (names cannot repeat) | Document is tree of named nodes (names can repeat) |
| No comments allowed | Comments allowed |
| Names are arbitrary strings in quotes | Names are strict tokens, no quotes needed |
| Document structured with curly brackets | Document structured with curly brackets or tabs |
| Fields are always separated by commas | Newlines imply new field, no separator character required |
| No line continuation syntax | Has line continuation syntax |
| No schema language | Has schema language |
| No path language | Has path language |
| Documents are standalone | Can define inter-dependencies between documents |
| Cannot define meta-information / tags | Can add meta-information / tags |