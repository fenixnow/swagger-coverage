[license]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"
[release]: https://github.com/viclovsky/swagger-coverage/releases/latest "Latest release"
[release-badge]: https://img.shields.io/github/release/viclovsky/swagger-coverage.svg?style=flat
[maven]: https://repo.maven.apache.org/maven2/com/github/viclovsky/swagger-coverage-commandline/ "Maven Central"
[maven-badge]: https://img.shields.io/maven-central/v/com.github/viclovsky/swagger-coverage-commandline.svg?style=flat

[![Build Status](https://github.com/viclovsky/swagger-coverage/workflows/Build/badge.svg)](https://github.com/viclovsky/swagger-coverage/actions)
[![release-badge][]][release]
[![maven-badge][]][maven] 

# swagger-coverage
Swagger-coverage даёт полную картину покрытия API-тестов (регресса) на основе спецификации OAS (Swagger).
Под покрытием понимается не функциональность в широком смысле, а наличие (или отсутствие) вызов, определённых методами API, параметрами, кодами возврата и другими условиями, соответствующими спецификации API.

![Swagger Coverage Report](.github/swagger-coverage.png)

## Как это работает
Создание отчёта о покрытии состоит из двух этапов. Во-первых, во время выполнения тестов фильтр/интерцептор/прокси сохраняет информацию о вызовах в формате swagger в специальную папку.
На втором этапе сохранённые результаты сравниваются с условиями, сгенерированными из текущей спецификации API, и строится отчёт.

## Как использовать и примеры
Swagger-coverage можно использовать с любым языком и фреймворком. Вам нужен прокси/фильтр/интерцептор, накапливающий данные в формате swagger.
Swagger-coverage имеет готовую интеграцию с REST Assured.

> Также доступна интеграция с Karate, документация к которой находится [здесь](/swagger-coverage-karate/README.md).

Добавьте зависимость фильтра:
```xml
 <dependency>
     <groupId>com.github.viclovsky</groupId>
     <artifactId>swagger-coverage-rest-assured</artifactId>
     <version>${latest-swagger-coverage-version}</version>
 </dependency>
```
или если используется gradle:

```
compile "com.github.viclovsky:swagger-coverage-rest-assured:$latest-swagger-coverage-version"
```

Просто добавьте фильтр SwaggerCoverageRestAssured (SwaggerCoverageV3RestAssured для v3) в тестовый клиент. Например:
```java
RestAssured.given().filter(new SwaggerCoverageRestAssured())
```

* Скачайте и запустите командную строку.
Скачайте zip-архив и распакуйте его. Не забудьте заменить {latest-swagger-coverage-version} на актуальную версию.
```
wget https://github.com/viclovsky/swagger-coverage/releases/download/{latest-swagger-coverage-version}/swagger-coverage-{latest-swagger-coverage-version}.zip
unzip swagger-coverage-commandline-{latest-swagger-coverage-version}.zip
```

Справка командной строки:

```
./swagger-coverage-commandline --help

  Options:
  * -s, --spec
      Path to local or URL to remote swagger specification.
  * -i, --input
      Path to folder with generated files with coverage.
    -c, --configuration
      Path to file with report configuration.
    --help
      Print commandline help.
    -q, --quiet
      Switch on the quiet mode.
      Default: false
    -v, --verbose
      Switch on the verbose mode.
      Default: false
```

Чтобы сравнить результаты API-тестов с текущей спецификацией и построить отчёт, запустите CLI-утилиту после выполнения тестов:

```
./swagger-coverage-commandline -s swagger.json -i swagger-coverage-output
```

Вывод команды:
```
19:21:21 INFO  OperationSwaggerCoverageCalculator - Empty coverage:
...
19:21:21 INFO  OperationSwaggerCoverageCalculator - Partial coverage:
...
19:21:21 INFO  OperationSwaggerCoverageCalculator - Full coverage:
...
19:21:21 INFO  OperationSwaggerCoverageCalculator - Conditions: 874/2520
19:21:21 INFO  OperationSwaggerCoverageCalculator - Empty coverage 49.284 %
19:21:21 INFO  OperationSwaggerCoverageCalculator - Partial coverage 12.034 %
19:21:21 INFO  OperationSwaggerCoverageCalculator - Full coverage 38.682 %
19:21:21 INFO  FileSystemResultsWriter - Write html report in file '.../swagger-coverage-report.html'
```
Результаты (swagger-coverage-report.html / swagger-coverage-results.json) будут созданы после запуска swagger-coverage.

## Настройки
Отчёт swagger-coverage можно настроить через JSON-файл.
Вы можете управлять списком проверяемых условий покрытия.

### Правила покрытия
Настройки правил размещаются в секции `"rules"`. Вы можете отключить отдельные правила или изменить их поведение.

Все правила **включены по умолчанию**. Каждое правило можно отключить или настроить индивидуально.
У каждого правила есть общие параметры:

| Параметр | Тип | По умолчанию | Описание |
|----------|-----|-------------|----------|
| `enable` | boolean | `true` | Включить или отключить правило |
| `filter` | массив строк | — | Проверять только элементы из списка (зависит от правила) |
| `ignore` | массив строк | — | Пропустить элементы из списка (зависит от правила) |

#### `status` — Коды ответа HTTP
**Идентификатор правила:** `status`

Создаёт отдельное условие для каждого кода ответа, объявленного в секции `responses` операции.
Условие помечается как *покрытое*, когда хотя бы один тестовый вызов вернул этот код ответа.

**Пример использования:** Проверить, что тесты затрагивают все объявленные коды ответов (200, 201, 400, 404, 500 и т.д.) для каждого эндпоинта.

```json
{
  "rules": {
    "status": {
      "enable": true,
      "filter": ["200", "201"],
      "ignore": ["500"]
    }
  }
}
```
*Этот пример проверяет только статусы `200` и `201`, игнорируя `500`.*

#### `only-declared-status` — Используются только объявленные статусы
**Идентификатор правила:** `only-declared-status`

Создаёт одно условие на операцию, проверяющее, что все полученные коды ответов объявлены в спецификации OpenAPI.
Условие помечается как *покрытое*, если не обнаружено необъявленных кодов ответов.

**Пример использования:** Обнаружить серверные ошибки (например, 500, 502), не задокументированные в спецификации, или определить отсутствующие коды ответов, которые стоит добавить.

```json
{
  "rules": {
    "only-declared-status": {
      "enable": true
    }
  }
}
```

#### `parameter-not-empty` — Параметр не пустой
**Идентификатор правила:** `parameter-not-empty`

Создаёт условие для каждого параметра операции (query, path, header). Условие помечается как *покрытое*, когда параметр присутствовал хотя бы в одном тестовом вызове.

**Пример использования:** Убедиться, что каждый объявленный query-параметр, path-параметр или заголовок фактически используется в тестах.

```json
{
  "rules": {
    "parameter-not-empty": {
      "enable": true
    }
  }
}
```

#### `empty-required-header` — Обязательный заголовок отсутствует
**Идентификатор правила:** `empty-required-header`

Создаёт условие для каждого заголовка (header). Условие помечается как *покрытое*, когда заголовок **не был** отправлен в запросе (пустой).

**Пример использования:** Проверить, что тесты покрывают сценарии с отсутствующими обязательными заголовками для проверки корректной обработки ошибок.

```json
{
  "rules": {
    "empty-required-header": {
      "enable": true
    }
  }
}
```

#### `enum-all-value` — Параметр покрывает все значения enum
**Идентификатор правила:** `enum-all-value`

Создаёт условие для каждого параметра с ограничением `enum`. Условие помечается как *покрытое* только когда **все** значения enum были получены по всем тестовым вызовам данной операции.

**Пример использования:** Для параметра `status` с enum `["active", "inactive", "pending"]` убедиться, что тесты покрывают все три значения.

```json
{
  "rules": {
    "enum-all-value": {
      "enable": true
    }
  }
}
```

#### `enum-another-value` — Значение параметра вне enum
**Идентификатор правила:** `enum-another-value`

Создаёт условие для каждого параметра с ограничением `enum`. Условие помечается как *покрытое*, когда хотя бы один тестовый вызов отправил значение, **не входящее** в enum.

**Пример использования:** Проверить, что тесты включают негативные сценарии — отправку невалидных enum-значений для проверки серверной валидации.

```json
{
  "rules": {
    "enum-another-value": {
      "enable": true
    }
  }
}
```

#### `not-empty-body` — Тело запроса не пустое
**Идентификатор правила:** `not-empty-body`

Создаёт условие для операций, объявляющих `requestBody`. Условие помечается как *покрытое*, когда хотя бы один тестовый вызов отправил непустое тело.

**Пример использования:** Убедиться, что POST/PUT операции с телом запроса тестируются с реальным содержимым тела.

```json
{
  "rules": {
    "not-empty-body": {
      "enable": true
    }
  }
}
```

#### `property-not-empty` — Свойство тела запроса не пустое
**Идентификатор правила:** `property-not-empty`

Создаёт условие для каждого свойства в схеме тела запроса. Условие помечается как *покрытое*, когда свойство присутствовало хотя бы в одном тестовом вызове.

**Пример использования:** Для объекта `User` с полями `name`, `email`, `role` убедиться, что тесты используют все поля.

```json
{
  "rules": {
    "property-not-empty": {
      "enable": true
    }
  }
}
```

#### `property-enum-all-value` — Свойство покрывает все значения enum
**Идентификатор правила:** `property-enum-all-value`

Создаёт условие для каждого свойства в теле запроса с ограничением `enum`. Условие помечается как *покрытое* только когда **все** значения enum были получены по всем тестовым вызовам.

**Пример использования:** Для поля `status` с enum `["draft", "published", "archived"]` проверить, что тесты покрывают все три состояния.

```json
{
  "rules": {
    "property-enum-all-value": {
      "enable": true
    }
  }
}
```

#### `property-enum-another-value` — Значение свойства вне enum
**Идентификатор правила:** `property-enum-another-value`

Создаёт условие для каждого свойства в теле запроса с ограничением `enum`. Условие помечается как *покрытое*, когда хотя бы один тестовый вызов отправил значение, **не входящее** в enum.

**Пример использования:** Проверить, что тесты включают негативные сценарии — отправку невалидных значений свойств для проверки валидации.

```json
{
  "rules": {
    "property-enum-another-value": {
      "enable": true
    }
  }
}
```

#### `exclude-deprecated` — Исключить устаревшие операции
**Идентификатор правила:** `exclude-deprecated`

Исключает устаревшие (deprecated) операции из категорий *Full*, *Partial* и *Empty* покрытия. Deprecated-операции не влияют на сводку "Operations coverage summary".

**Пример использования:** Когда нужно измерять покрытие только актуальных операций, не учитывая устаревшие.

**Отключено по умолчанию** — необходимо явно включить:

```json
{
  "rules": {
    "exclude-deprecated": {
      "enable": true
    }
  }
}
```

### Полный пример конфигурации

Этот пример включает все правила, фильтрует проверку статусов только по `200` и `201`, игнорирует `400` и `500`, а также настраивает HTML, JSON и LLM writers:

```json
{
  "rules": {
    "status": {
      "filter": ["200", "201"],
      "ignore": ["400", "500"]
    },
    "only-declared-status": {
      "enable": true
    },
    "parameter-not-empty": {
      "enable": true
    },
    "empty-required-header": {
      "enable": true
    },
    "enum-all-value": {
      "enable": true
    },
    "enum-another-value": {
      "enable": true
    },
    "not-empty-body": {
      "enable": true
    },
    "property-not-empty": {
      "enable": true
    },
    "property-enum-all-value": {
      "enable": true
    },
    "property-enum-another-value": {
      "enable": true
    },
    "exclude-deprecated": {
      "enable": true
    }
  },
  "writers": {
    "html": {
      "filename": "swagger-coverage-report.html",
      "locale": "en"
    },
    "json": {
      "filename": "swagger-coverage-results.json"
    },
    "llm": {
      "filename": "swagger-coverage-llm-report.json"
    }
  }
}
```

Если вам нужны свои правила для генерации условий, пожалуйста, присылайте PR.

### JSON-отчёт для LLM

Для генерации компактного отчёта, оптимизированного для анализа LLM, используйте writer `llm`. Формат группирует данные по методам API и включает описания требований из swagger-спецификации.

**Пример выходного формата:**

```json
{
  "api": "API v1.0",
  "summary": { "total_operations": 89, "not_covered": 64, "coverage_percent": 13.0 },
  "paths": {
    "/category": {
      "GET": {
        "state": "PARTY",
        "coverage": "20/21",
        "requirements": {
          "parameters": [
            { "name": "with_ft", "in": "query", "description": "Флаг для получения товаров с falseteasers", "covered": false }
          ]
        }
      }
    }
  }
}
```

Этот формат позволяет LLM запросить конкретный метод и получить все требования с описаниями.

## Настройка вывода результатов
Параметры генерации отчёта размещаются в секции *writers*.

### HTML-отчёт
Настройки HTML-отчёта размещаются в подсекции *html* секции *writers*.

Доступные параметры:

**locale** — двухбуквенный код языка. Поддерживаются *en/ru*.

**filename** — имя файла HTML-отчёта.

**numberFormat** — [расширенный формат чисел Java](https://freemarker.apache.org/docs/ref_builtins_number.html#topic.extendedJavaDecimalFormat) для управления отображением чисел в отчёте.

````
{
  ....

  "writers": {
      "html": {
        "locale": "ru",
        "filename":"report.html",
        "numberFormat": "0.##"
      }
  }
}
`````

### JSON-отчёт для LLM

Для генерации компактного отчёта, оптимизированного для анализа LLM, используйте writer `llm`. Формат группирует данные по методам API и включает описания требований из swagger-спецификации.

**Пример выходного формата:**

```json
{
  "api": "API v1.0",
  "summary": { "total_operations": 89, "not_covered": 64, "coverage_percent": 13.0 },
  "paths": {
    "/category": {
      "GET": {
        "state": "PARTY",
        "coverage": "20/21",
        "requirements": {
          "parameters": [
            { "name": "with_ft", "in": "query", "description": "Флаг для получения товаров с falseteasers", "covered": false }
          ]
        }
      }
    }
  }
}
```

Этот формат позволяет LLM запросить конкретный метод и получить все требования с описаниями.

### Настройка шаблона отчёта
Для кастомизации HTML-отчёта собственным шаблоном укажите полный путь к шаблону:
````
{
  ....
  
  "writers": {
    "html": {
      ....
      "customTemplatePath": "/full/path/to/report_custom.ftl"
    }
  }
}
`````

[Смотрите здесь](https://github.com/swagger-api/swagger-parser/blob/master/modules/swagger-parser-core/src/main/java/io/swagger/v3/parser/core/models/ParseOptions.java) все доступные опции.

## Использование Docker

Вы можете собрать Docker-образ swagger-coverage локально для запуска CLI-утилиты в контейнере.

### Сборка Docker-образа

Находясь в корневой директории проекта, выполните:

```bash
docker build -t swagger-coverage:local .
```

### Запуск CLI-утилиты в Docker

После сборки образа вы можете запустить swagger-coverage в контейнере:

**Базовое использование:**
```bash
docker run --rm -v $(pwd)/swagger-coverage-output:/data swagger-coverage:local \
  -s /data/swagger.json -i /data/coverage-output
```

**С файлом конфигурации:**
```bash
docker run --rm -v $(pwd):/data swagger-coverage:local \
  -s /data/swagger.json \
  -i /data/swagger-coverage-output \
  -c /data/config.json
```

**Просмотр справки:**
```bash
docker run --rm swagger-coverage:local --help
```

**Пояснение параметров:**
- `-v $(pwd):/data` — монтирует текущую директорию в `/data` внутри контейнера
- `-s /data/swagger.json` — путь к спецификации (относительно смонтированной директории)
- `-i /data/swagger-coverage-output` — путь к директории с результатами покрытия
- `-c /data/config.json` — путь к файлу конфигурации (опционально)

### Особенности Docker-образа

- **Базовый образ:** Eclipse Temurin 8 JRE (сборка — `gradle:8.14-jdk8`)
- **Пользователь:** работает под пользователем `swagger` (не root) для безопасности
- **Рабочая директория:** `/data`
- **Точка входа:** автоматически использует `swagger-coverage-commandline`

### GitHub Actions CI/CD

Проект использует GitHub Actions для автоматической сборки:

- **CI** (`.github/workflows/build.yml`) — сборка проекта на pull requests и push в master
- **Release** (`.github/workflows/release.yml`) — публикация артефактов при создании релиза, включая:
  - Публикацию в Maven Central
  - Генерацию и публикацию ZIP/TAR архивов на GitHub Releases

Архивы для скачивания создаются автоматически при релизе и доступны на странице [Releases](https://github.com/viclovsky/swagger-coverage/releases/latest).


## Demo
Подготовлено несколько тестов. Вы можете посмотреть и попробовать swagger-coverage. Просто запустите скрипт ```run.sh```.

## Важное замечание
Swagger-coverage корректно работает с клиентами, сгенерированными из swagger (например: https://github.com/OpenAPITools/openapi-generator).
Поскольку все методы/параметры, которые будут сохранены, на 100% совместимы с текущей спецификацией API.

## Требования

В данный момент swagger-coverage совместим только со спецификациями OpenAPI v2 и v3. Возможно, в будущем будут поддержаны и другие версии.

## Pull Requests
Проект открыт для любых улучшений. Ваша помощь очень ценится. Не стесняйтесь создавать pull request или issue — я рассмотрю их в течение нескольких дней.

## Создатель и сопровождающий
[Victor Orlovsky](https://github.com/viclovsky)

## Участие в разработке
Спасибо всем, кто внёс вклад. Особая благодарность

* [@TemaMak](https://github.com/TemaMak)
* [@Emilio-Pega](https://github.com/Emilio-Pega)

за значительные улучшения swagger-coverage.

## Лицензия
Swagger coverage выпущен под версией 2.0 [лицензии Apache](http://www.apache.org/licenses/LICENSE-2.0)