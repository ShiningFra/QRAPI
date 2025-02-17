---

# QRAPI

L'API de génération et de scan de QR codes pour un système d'authentification handshake et de gestion de courses.

---

## 1. Présentation Générale

La **QRAPI** est une API REST développée en Spring Boot qui permet de :

- **Générer des QR codes** contenant des informations relatives à une course (identifiants client, chauffeur, course, localisation, etc.)
- **Scanner et valider ces QR codes** pour confirmer une course ou enregistrer une action (par exemple, mettre à jour l’historique d’un scan)
- **Sécuriser l’accès aux endpoints** via l’authentification par token JWT

> **Note sur l'authentification :**  
> Bien que l’exemple présente ici les endpoints pour générer et scanner un QR code, l’obtention d’un token JWT (via l’endpoint `/api/reserve`) est nécessaire pour sécuriser l’accès. Cet endpoint d’authentification est attendu dans l’architecture globale mais n’est pas détaillé dans ce contrôleur.

Chaque utilisateur doit obtenir un token JWT (généralement via `/api/reserve`) et l’inclure dans l’en-tête HTTP :

```
Authorization: Bearer <TOKEN>
```

lors des appels aux endpoints suivants.

---

## 2. Endpoints Principaux

### a) Authentification – Réservation du Token  
**Remarque :** Cet endpoint n'est pas implémenté dans ce contrôleur. Il est supposé exister dans le cadre de l’API globale pour générer un token JWT.  
- **URL :** `/api/reserve`  
- **Méthode :** `POST`  
- **Paramètres :**  
  - `fournisseur` (passé dans le corps de la requête ou en query string)  
- **Exemple d’appel avec cURL :**
  ```bash
  curl -X POST "http://localhost:8080/api/reserve?fournisseur=Fra%20Yuuki" \
       -H "Content-Type: application/json"
  ```
- **Réponse attendue :**
  ```json
  {
      "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJGcmEgWXV1a2kiLCJpYXQiOjE2MzMyNDk5OTl9.XXXXXXXXXXXXXXX"
  }
  ```

---

### b) Génération du QR Code  
- **URL :** `/api/qr/generate`  
- **Méthode :** `POST`  
- **Paramètres :**
  - **Query parameters :**
    - `secret` : La clé utilisée pour signer le QR code (doit respecter la longueur minimale pour HS256, généralement 32 caractères ou plus)
    - `expirationMillis` : La durée de validité (en millisecondes) du token signé qui sera intégré dans le QR code
  - **Corps (JSON) :**  
    Un objet de type `QRData` contenant par exemple :
    ```json
    {
      "clientId": 123456,
      "chauffeurId": 654321,
      "courseId": 987654,
      "lieu": "Gare Centrale",
      "heure": "14:30",
      "date": "2025-02-07",
      "ville": "Paris",
      "pays": "France",
      "fournisseur": "NomDuFournisseur"
    }
    ```
  - **Authentification :**  
    Le token JWT (obtenu via `/api/reserve`) doit être transmis dans l’en-tête :
    ```
    Authorization: Bearer <TOKEN>
    ```
- **Fonctionnement :**
  1. Le contrôleur attribue à l’objet `QRData` un identifiant unique (UUID) et enregistre le nom du fournisseur extrait du `Principal`.
  2. Les données de `QRData` sont converties en chaîne (par exemple via `toString()`) puis hachées avec l’algorithme SHA-256.
  3. Le hash est signé en créant un token JWT à l’aide de la clé `secret` et de la durée de validité définie par `expirationMillis`.
  4. Le token signé est converti en image QR au format PNG grâce à la bibliothèque ZXing.
- **Exemple d’appel cURL :**
  ```bash
  curl -X POST "http://localhost:8080/api/qr/generate?secret=MaSuperCleSecrete&expirationMillis=3600000" \
       -H "Content-Type: application/json" \
       -H "Authorization: Bearer <TOKEN>" \
       -d '{
             "clientId": 123456,
             "chauffeurId": 654321,
             "courseId": 987654,
             "lieu": "Gare Centrale",
             "heure": "14:30",
             "date": "2025-02-07",
             "ville": "Paris",
             "pays": "France",
             "fournisseur": "NomDuFournisseur"
           }'
  ```
- **Réponse :**  
  Le serveur renvoie une image PNG (sous forme de bytes) contenant le QR code généré. Le header de la réponse est `Content-Type: image/png`.

---

### c) Scan du QR Code  
- **URL :** `/api/qr/scan`  
- **Méthode :** `POST`  
- **Paramètres :**
  - **Query parameters :**
    - `qrCodeData` : Le token JWT issu du QR code contenant le hash signé
    - `secret` : La clé utilisée pour vérifier la signature du token
  - **Corps (JSON) :**  
    Un objet `History` qui contient les informations supplémentaires à enregistrer lors du scan (par exemple, des champs personnalisés relatifs à l’opération)
  - **Authentification :**  
    Le header HTTP doit contenir :
    ```
    Authorization: Bearer <TOKEN>
    ```
- **Fonctionnement :**
  1. Le contrôleur décode et vérifie la signature du token contenu dans `qrCodeData` en utilisant la clé `secret`.  
     - Si la signature n'est pas valide, la réponse est un HTTP 401 avec le message « QR Code invalide ! ».
  2. En cas de signature valide, le service de scan (via `ScanService`) récupère l’objet `QRHash` associé.
  3. À partir du `QRHash`, le contrôleur recherche les données complètes dans la table `QRData`.
  4. L’objet `History` est complété avec les informations extraites de `QRData` (par exemple `client_id`, `driver_id`, `trip_id`, `fournisseur`) et est sauvegardé dans la base.
- **Exemple d’appel cURL :**
  ```bash
  curl -X POST "http://localhost:8080/api/qr/scan?qrCodeData=<JWT_QR_CODE>&secret=MaSuperCleSecrete" \
       -H "Content-Type: application/json" \
       -H "Authorization: Bearer <TOKEN>" \
       -d '{
             "someHistoryField": "valeur",
             "autreChamp": "valeur"
           }'
  ```
- **Réponse :**  
  - En cas de succès, le serveur renvoie l’objet `QRData` correspondant (ou `null` s'il n'est pas trouvé) au format JSON avec un status HTTP 200.
  - En cas d'erreur de signature, une réponse HTTP 401 est renvoyée.
  - En cas d'exception, une réponse HTTP 400 avec un message d'erreur est renvoyée.

---

## 3. Guide de Déploiement

### 3.1. Prérequis
- **Java 11 ou supérieur** (idéalement Java 17 ou 23)
- **Maven** ou **Gradle** pour la construction
- **Cassandra** (installé localement, en Docker ou sur un cluster distant)
- **Docker** (optionnel, pour containeriser l’application et/ou la base de données)

### 3.2. Configuration et Préparation
1. **Configurer la base de données Cassandra**  
   Assurez-vous que Cassandra est installé et accessible.  
   Exemple avec Docker :
   ```bash
   docker run --name cassandra -d -p 9042:9042 cassandra:latest
   ```
   Puis, dans `cqlsh`, créez (si nécessaire) le keyspace :
   ```cql
   CREATE KEYSPACE IF NOT EXISTS transportapp WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
   ```

2. **Configurer l’application**  
   Dans le fichier `src/main/resources/application.properties` (ou `application.yml`), configurez Cassandra :
   ```properties
   spring.application.name=QRAPI
   spring.data.cassandra.contact-points=127.0.0.1
   spring.data.cassandra.port=9042
   spring.data.cassandra.keyspace-name=transportapp
   spring.data.cassandra.local-datacenter=datacenter1
   spring.data.cassandra.schema-action=create-if-not-exists
   server.port=8080
   ```

3. **Vérifier les dépendances Maven**  
   Assurez-vous d’inclure dans votre `pom.xml` les dépendances suivantes (ainsi que celles nécessaires pour JJWT et ZXing) :
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-cassandra</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-web</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-security</artifactId>
   </dependency>
   <!-- Dépendances pour JJWT, ZXing, etc. -->
   ```

### 3.3. Construction et Lancement
1. **Construire l’application**  
   Avec Maven, exécutez :
   ```bash
   mvn clean install
   ```
2. **Lancer l’application**  
   Exécutez :
   ```bash
   java -jar target/QRAPI-0.0.1-SNAPSHOT.jar
   ```
   L’application sera accessible sur `http://localhost:8080`.

### 3.4. Déploiement via Docker (Optionnel)
1. **Créer un Dockerfile** à la racine du projet :
   ```dockerfile
   FROM openjdk:17-jdk-alpine
   COPY target/QRAPI-0.0.1-SNAPSHOT.jar /app/QRAPI.jar
   WORKDIR /app
   EXPOSE 8080
   CMD ["java", "-jar", "QRAPI.jar"]
   ```
2. **Construire l'image Docker** :
   ```bash
   docker build -t qrapi .
   ```
3. **Lancer le conteneur Docker** :
   ```bash
   docker run -p 8080:8080 qrapi
   ```

---

## 4. Modèle Mathématique et Cas d'Utilisation

### 4.1. Modèle Mathématique

#### Entités
- **Chauffeurs (C)**  
  \( C = \{ c \mid c = (id_c, nom, \dots ) \} \)
- **Clients (L)**  
  \( L = \{ l \mid l = (id_l, nom, \dots ) \} \)
- **Courses (R)**  
  \( R = \{ r \mid r = (id_r, id_chauffeur, id_client, lieu, heure, date, ville, pays) \} \)
- **QR Codes (Q)**  
  \( Q = \{ q \mid q = (id_q, hash, id_course) \} \)
- **Historique de Scans (H)**  
  \( H = \{ h \mid h = (id_h, id_course, id_chauffeur, id_client, timestamp) \} \)

#### Fonctions
- **Génération du QR Code**  
  Une fonction \( f: R \to Q \) qui prend une course et génère un QR code unique, en calculant :
  - Un hash des données (via SHA-256)
  - Un token signé (JWT) avec une clé secrète et une durée d’expiration
- **Validation du QR Code**  
  Une fonction \( g: Q \to \{ \text{valide}, \text{invalide} \} \) qui, en vérifiant la signature du token, détermine la validité du QR code.

---

### 4.2. Cas d'Utilisation

#### Scénario 1 : Génération d'un QR Code
- **Acteur :** Fournisseur (ex. chauffeur)
- **Préconditions :**
  - La course est créée et enregistrée.
  - L’utilisateur est authentifié (token JWT valide obtenu via `/api/reserve`).
- **Flux Principal :**
  1. L’utilisateur envoie une requête `POST /api/qr/generate` avec les données de la course, le secret et la durée d’expiration.
  2. Le système attribue un UUID à la course, calcule un hash, signe ce hash et génère le QR code correspondant.
  3. Le QR code (image PNG) est renvoyé au client.
- **Postconditions :**
  - Le QR code ainsi que les informations associées (dans `QRData` et `QRHash`) sont enregistrés dans la base.

#### Scénario 2 : Scan et Validation d'un QR Code
- **Acteur :** Fournisseur (ex. chauffeur ou agent de vérification)
- **Préconditions :**
  - Le QR code a été généré et est en possession de l’utilisateur.
  - L’utilisateur est authentifié (token JWT valide).
- **Flux Principal :**
  1. L’utilisateur scanne le QR code et envoie une requête `POST /api/qr/scan` avec :
     - Le token JWT issu du QR code (`qrCodeData`)
     - Le même secret utilisé pour la signature
     - Un objet `History` contenant des informations supplémentaires
  2. Le système vérifie la signature du token.
  3. En cas de succès, il récupère les données associées (via `QRHash` et `QRData`) et met à jour l’historique.
  4. Le système renvoie l’objet `QRData` correspondant (ou `null` s'il n'est pas trouvé).
- **Flux Alternatif :**
  - Si la signature est invalide ou le QR code n’est pas trouvé, une erreur (HTTP 401 ou 404) est renvoyée.
- **Postconditions :**
  - L’opération de scan est enregistrée dans la base (via `History`).

---

## 5. Structure du Code et Diagramme de Classes

### 5.1. Principales Classes
- **QRData**  
  Contient les informations de la course (client, chauffeur, course, lieu, heure, date, ville, pays, fournisseur).
- **QRHash**  
  Enregistre le hash signé associé à une instance de `QRData` et la référence à son identifiant.
- **History**  
  Stocke l’historique des scans avec les informations suivantes :
  - `id` (UUID)
  - `client_id` (Long)
  - `driver_id` (Long)
  - `trip_id` (Long)
  - `location` (String)
  - `supplier` (String)
  - `hour` (String)
  - `date` (String)
  - `city` (String)
  - `country` (String)

### 5.2. Services et Contrôleurs
- **QRCodeController**  
  Gère les endpoints `/api/qr/generate` et `/api/qr/scan`.
- **ScanService**  
  Contient la logique métier de validation et de traitement des scans.
- **JwtUtil**  
  (Optionnel) Gère la signature et la vérification des tokens JWT.

### 5.3. Diagramme de Classes (UML Simplifié)
```
+----------------+       +----------------+       +----------------+
|    QRData      |       |    QRHash      |       |    History     |
+----------------+       +----------------+       +----------------+
| - id : UUID    |<>-----| - id : UUID    |       | - id : UUID    |
| - client_id    |       | - hash : String|       | - client_id    |
| - driver_id    |       | - qr_data_id   |       | - driver_id    |
| - trip_id      |       +----------------+       | - trip_id      |
| - location     |                              | - location     |
| - hour         |                              | - supplier     |
| - date         |                              | - hour         |
| - city         |                              | - date         |
| - country      |                              | - city         |
| - fournisseur  |                              | - country      |
+----------------+                              +----------------+

            +--------------------+
            |  QRCodeController  |
            +--------------------+
            | - qrDataRepository |
            | - qrHashRepository |
            | - historyRepository|
            | - scanService      |
            +--------------------+
                     ^
                     |
            +--------------------+
            |     ScanService    |
            +--------------------+
            |  (Logique de scan) |
            +--------------------+
```
