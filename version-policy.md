---
icon: code-branch
---

# Version Policy



## SocketIO4j Version Policy

`socketio4j` follows **Semantic Versioning** using the format:

```
x.y.z  →  MAJOR.MINOR.PATCH
```

### Version Number Semantics

#### **MAJOR (x)**

Incremented when:

* Breaking API changes are introduced
* Protocol behavior changes
* Major architectural redesigns occur

#### **MINOR (y)**

Incremented when:

* New features or APIs are added
* Backward compatibility is preserved

#### **PATCH (z)**

Incremented when:

* Bug fixes are applied
* Performance improvements are made
* Internal refactors do not affect public APIs

### Major Version Strategy

#### **3.x – Compatibility Line**

* Maintains compatibility with the parent fork
* Minimal divergence from upstream
* Focused on stability and bug fixes
* No major API redesigns

```
3.y.z
```

#### **4.x – LTS Line (Upcoming)**

* Introduces new APIs and extended functionality
* Includes architectural and performance improvements
* Designed for production and enterprise use
* Backward compatibility maintained within the 4.x line

> **Current status:**\
> `4.0.0` is currently in **SNAPSHOT** and under active development.\
> Once released as GA, **4.x will become the Long-Term Support (LTS) line**.

```
4.0.0-SNAPSHOT   ← Current
4.y.z            ← Future LTS releases
```

#### **5.x – Development Line (Planned)**

* Forward-looking and experimental
* May introduce breaking changes
* Rapid iteration and feature exploration
* Not recommended for production use

```
5.0.0-SNAPSHOT
```

### Release Qualifiers

Pre-release versions are identified using postfix qualifiers **before GA**:

| Qualifier      | Description                  |
| -------------- | ---------------------------- |
| `-SNAPSHOT`    | Active development, unstable |
| `-M1`, `-M2`   | Milestone releases           |
| `-RC1`, `-RC2` | Release candidates           |
| _(no postfix)_ | General Availability (GA)    |

#### **Release Flow Example**

```
4.0.0-SNAPSHOT
4.0.0-M1
4.0.0-RC1
4.0.0        ← GA (LTS)
```

***

### Stability & Support Guarantees

* **GA releases** are production-ready
* **Patch releases (`x.y.z`)** are backward compatible
* **Minor releases (`x.y`)** add features without breaking APIs
* **Major releases (`x`)** may introduce breaking changes
* **LTS (4.x)** will receive:
  * Critical bug fixes
  * Security updates
  * Long-term maintenance

> - SNAPSHOT versions should **never** be used in production
> - Milestone (`-M`) and Release Candidate (`-RC`) versions are intended for validation and feedback
> - Only **GA releases** are considered fully stable

