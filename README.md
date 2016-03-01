# GsonValue

Compile-time generation of gson TypeAdapters to preserve class encapsulation. 

By default, gson uses reflection to read and write fields of you class from json. The problem with
this is that it breaks encapsulation. This prevents you from enforcing invariants when you class is
constructed and properly hide implementation details from serialization. This library will generate
TypeAdapters the call your constructor, factory method, or builder to construct you an instance of
your class and will only use accessible fields and methods to write it out.

This library is a nice companion with google's
[AutoValue](https://github.com/google/auto/tree/master/value), but you may use it with any class you
choose.

## Download

### Maven
```xml
<dependency>
  <groupId>me.tatarka.gsonvalue</groupId>
  <artifactId>gsonvalue</artifactId>
  <version>0.3</version>
</dependency>
<dependency>
  <groupId>me.tatarka.gsonvalue</groupId>
  <artifactId>gsonvalue-processor</artifactId>
  <version>0.3</version>
  <scope>provided</scope>
</dependency>
```

### Gradle

You may want to use a gradle plugin for easier management of apt dependencies. For example,
[gradle-apt-plugin](https://github.com/tbroyer/gradle-apt-plugin) for plain java or
[android-apt](https://bitbucket.org/hvisser/android-apt) for android.

```groovy
apt 'me.tatarka.gsonvalue:gsonvalue-processor:0.3'
compile 'me.tatarka.gsonvalue:gsonvalue:0.3'
```

## Usage 

Annotate your constructor or factory method with `@GsonConstructor`. Json will be deserialized using
the parameter names and serialized using public fields or getters. The following classes will map to
the json
```json
{"arg":1}
```

```java
import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class Foo {
    @GsonConstructor
    public Foo(int arg) {
        ...
    }
    
    public int arg() {
        ...
    }
}

public class Bar {
    @GsonConstructor
    public static Bar newInstance(int arg) {
        ...
    }
    
    public int getArg() {
        ...
    }
}
```
Note that both bare getters and bean-style prefixes are supported. Like AutoValue, prefixes only
apply if all getters follow that style.

### Builders

Alternatively, you can create a builder class. Annotate either the builder's constructor or
 the factory method that returns the builder with `@GsonBuilder`.

```java
import me.tatarka.gsonvalue.annotations.GsonConstructor;

public class Foo {
    public static class Builder {
        @GsonBuilder
        public Builder(int arg1) {
            ...
        }
        
        public Builder arg2(String arg2) {
            ...
        }
        
        public Foo build() {
            ...
        }
    }
}

public class Bar {
    @GsonBuilder
    public static Builder builder() {
        ...
    }
    
    public static class Builder {
        ...
    }
}
```

In most cases the class being built will be discovered from the builder. However, it's possible that
it is ambiguous. In that case, you can provide the built class in the annotation
`@GsonBuilder(Foo.class)`.

### Gson

Finally register `ValueTypeAdapterFactory` to you gson builder.
```java
gson = new GsonBuilder()
        .registerTypeAdapterFactory(new ValueTypeAdapterFactory())
        .create();
```

### Supported Gson features.

* `@SerializeName` is supported on fields or getters. It will map to both the constructor
parameter on deserialization and the field or getter on serialization.
* Transient fields are ignored.

### Unsupported features.

The following features are not supported. They may be added if there is enough demand.

* `@Adapter` annotations.
* `@Since` and `@Until` versioning support.
* `@Expose` and exclusion strategies.
* Field naming policies.

## License

    Copyright 2016 Evan Tatarka
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
