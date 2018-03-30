# TreeML
Micro ML parser with validation and expression language.

## Comparison with JSON

| JSON | TreeML |
|---|---|
| Number, Boolean, String, Array, Object | Integer, Double, Boolean, Token, DateTime, Duration, String, List |
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

## Basic document

```
address {
    number : "37a"
    street : "Main Street"
    line : "Megacity One"
    line : "Eastern Wasteland"
    zip : "12345ABC"
    country : "Narnia"
    email : "abc@example.com", "ab.cee@example.com"
    phone {
        land : "00 99 1234 56789"
        mobile : "00 99 9876 56789"
    }
}
```

## Basic schema

```
#treeml:type:schema
#treeml:id::address
address : single, {
    number : string, single
    street : string, single
    line : string, optional
    zip : string, single
    country : string, single
    email : single, optional, list, string
    phone : empty, {
        land : string, optional, single
        mobile : string, optional, single
    }
}
```