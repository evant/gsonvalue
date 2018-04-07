# GsonValue

Compile-time generation of gson TypeAdapters to preserve class encapsulation. 

By default, gson uses reflection to read and write fields of you class from json. The problem with
this is that it breaks encapsulation. This prevents you from enforcing invariants when you class is
constructed and properly hide implementation details from serialization. This library will generate
TypeAdapters the call your constructor, factory method, or builder to construct you an instance of
your class and will only use accessible fields and methods to write it out.

This library is a nice companion with google's
[AutoValue](https://github.com/google/auto/tree/master/value), but you may use it with any class you
choose. It's nice with kotlin's [data](https://kotlinlang.org/docs/reference/data-classes.html) 
classes too!

## Download

### Gradle

#### Java
You may want to use a gradle plugin for easier management of apt dependencies. For example,
[gradle-apt-plugin](https://github.com/tbroyer/gradle-apt-plugin).

```groovy
apt 'me.tatarka.gsonvalue:gsonvalue-processor:0.9'
compileOnly 'me.tatarka.gsonvalue:gsonvalue-annotations:0.9'
```

#### Android
```groovy
annotationProcessor 'me.tatarka.gsonvalue:gsonvalue-processor:0.9'
compileOnly 'me.tatarka.gsonvalue:gsonvalue-annotations:0.9'
```

### Maven
```xml
<dependency>
  <groupId>me.tatarka.gsonvalue</groupId>
  <artifactId>gsonvalue-annotations</artifactId>
  <version>0.9</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>me.tatarka.gsonvalue</groupId>
  <artifactId>gsonvalue-processor</artifactId>
  <version>0.9</version>
  <scope>provided</scope>
</dependency>
```


## Usage 

Annotate your constructor or factory method with `@GsonConstructor`. Json will be deserialized using
the parameter names and serialized using public fields or getters. The following classes will map to
the json
```json
{"arg":1}
```

#### POJO
```java
import me.tatarka.gsonvalue.annotations.GsonValue;

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

#### AutoValue
```java
@AutoValue
public abstract class Foo {
    @GsonConstructor
    public static Foo newInstance(int arg) {
        return new AutoValue_Foo(arg);
    }
    
    public abstract int arg();
}
```

#### Kotlin Data Class
```kotlin
@GsonConstructor data class Foo(val arg: Int)
```

Note that both bare getters and bean-style prefixes are supported. Like AutoValue, prefixes only
apply if all getters follow that style.

### Builders

Alternatively, you can create a builder class. Annotate either the builder's constructor or
 the factory method that returns the builder with `@GsonBuilder`.

#### POJO
```java
import me.tatarka.gsonvalue.annotations.GsonValue;

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

#### AutoValue
```java
@AutoValue
public abstract class Foo {

    @GsonBuilder
    public static Builder builder() {
        return new AutoValue_Foo.Builder();
    }

    @AutoValue.Builder
    public interface Builder {        
        Builder arg(int arg);
        
        Foo build();
    }
}
```

### Gson

Create an abstract `TypeAdapterFactory` class an annotate it.
```java
import com.google.gson.TypeAdapterFactory;
import me.tatarka.gsonvalue.annotations.GsonValueTypeAdapterFactory;

@GsonValueTypeAdapterFactory
public abstract class MyTypeAdapterFactory implements TypeAdapterFactory {
    public static MyTypeAdapterFactory create() {
        return new GsonValue_MyTypeAdapterFactory();
    }
}
```

Then register it to your gson builder.
```java
gson = new GsonBuilder
        .registerTypeAdapterFactory(MyTypeAdapterFactory.create())
        .create();
```

### Supported Gson features.

* `@SerializeName` is supported on fields or getters. It will map to both the constructor
parameter on deserialization and the field or getter on serialization.
* Transient fields are ignored.
* `@JsonAdapter` is supported with either a `TypeAdapter` or `TypeAdapterFactory` as long as it
has a public no-args constructor. Since gson's version cannot be placed on a getter method, you may
use `@me.tatarka.gsonvalue.annotations.JsonAdapter` instead if necessary.

### Unsupported features.

The following features are not supported. They may be added if there is enough demand.

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
