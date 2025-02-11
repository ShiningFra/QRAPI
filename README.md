# QRAPI
L'API d'Authentification Handshake à travers les QRCode
---

## 1. Guide d'Utilisation de la QRAPI

### 1.1. Présentation générale

La **QRAPI** est une API REST développée en Spring Boot destinée à :

- **Générer des QR codes** contenant des informations relatives à des courses (identifiants de client, chauffeur, course, etc.)  
- **Scanner et valider ces QR codes** pour confirmer une course ou enregistrer une action (ex. : un scan validé met à jour l’historique de la course)  
- **Assurer l’authentification** via un endpoint dédié (/api/reserve) qui génère un token JWT pour sécuriser l’accès aux autres endpoints de l’API.

Chaque utilisateur de l’API devra d’abord appeler l’endpoint d’authentification (/api/reserve) pour obtenir un token qu’il devra inclure dans l’en-tête `Authorization: Bearer <TOKEN>` lors de l’appel des autres endpoints.

### 1.2. Endpoints Principaux

#### a) Authentification – Réservation du Token  
- **URL** : `/api/reserve`  
- **Méthode** : `POST`  
- **Paramètres** :  
  - `fournisseur` (passé en paramètre, par exemple dans le corps de la requête ou en query string)  
- **Fonctionnement** :  
  - L’API génère un token JWT avec le champ `provider` (subject) égal au nom du fournisseur.
- **Exemple d’appel avec cURL** :
  ```bash
  curl -X POST "http://localhost:8080/api/reserve?fournisseur=Fra%20Yuuki" \
       -H "Content-Type: application/json"
  ```
- **Réponse** :
  ```json
  {
      "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJGcmEgWXV1a2kiLCJpYXQiOjE2MzMyNDk5OTl9.XXXXXXXXXXXXXXX"
  }
  ```

#### b) Génération du QR Code  
- **URL** : `/api/qr/generate`  
- **Méthode** : `POST`  
- **Paramètres** (via URL et Body) :  
  - **Query parameters** :
    - `secret` : La clé utilisée pour signer le QR code (par exemple, une clé sécurisée partagée ou fixée côté serveur).
    - `expirationMillis` : La durée de validité (en millisecondes) du QR code.
  - **Corps (JSON)** :  
    Un objet de type `QRData` comportant par exemple :
    ```json
    {
      "clientId": 123456,
      "chauffeurId": 654321,
      "courseId": 987654,
      "lieu": "Gare Centrale",
      "heure": "14:30",
      "date": "2025-02-07",
      "ville": "Paris",
      "pays": "France"
    }
    ```
  - **Authentification** :  
    Le token JWT doit être envoyé dans l'en-tête HTTP  
    `Authorization: Bearer <TOKEN>` (celui obtenu via /api/reserve).  
- **Fonctionnement** :  
  - Le contrôleur attribue à l’objet QRData un identifiant unique (UUID) et enregistre le fournisseur (extrait du `Principal` ou `Authentication`).
  - Les données de QRData sont converties en une chaîne (par exemple en JSON) et hachées via SHA-256.
  - Le hash est ensuite signé avec la clé `secret` et la validité est limitée par `expirationMillis` pour produire un token signé.
  - Le token signé est ensuite transformé en QR code (image PNG) à l’aide de ZXing.
- **Exemple d’appel cURL** :
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
             "pays": "France"
           }'
  ```
- **Réponse** :  
  Le serveur renvoie une image PNG (représentée en bytes ou en base64 selon la configuration) contenant le QR code.

#### c) Scan du QR Code  
- **URL** : `/api/qr/scan`  
- **Méthode** : `POST`  
- **Paramètres** :  
  - **Query parameter** :  
    - `qrCodeData` : Le token signé contenu dans le QR code (encodé en JWT).
    - `secret` : La clé utilisée pour vérifier la signature.
  - **Corps (JSON)** :  
    Un objet `History` contenant les informations à enregistrer (ex. : le résultat du scan, IDs, etc.).
  - **Authentification** :  
    L'en-tête HTTP `Authorization: Bearer <TOKEN>` doit être présent.
- **Fonctionnement** :  
  - Le contrôleur décode et vérifie le token (via la méthode `verifySignature`).
  - Si la signature est valide, le service de scan (ScanService) récupère l'objet `QRHash` associé.
  - Le contrôleur utilise cet objet pour rechercher les données complètes dans la table `QRData`.
  - Ensuite, il met à jour l'historique des scans (HistoryRepository) et renvoie un message de succès.
- **Exemple d’appel cURL** :
  ```bash
  curl -X POST "http://localhost:8080/api/qr/scan?qrCodeData=<JWT_QR_CODE>&secret=MaSuperCleSecrete" \
       -H "Content-Type: application/json" \
       -H "Authorization: Bearer <TOKEN>" \
       -d '{
             "someHistoryField": "valeur",
             "autreChamp": "valeur"
           }'
  ```
- **Réponse** :
  ```json
  {
      "message": "Scan réussi"
  }
  ```

---

## 2. Guide de Déploiement de la QRAPI

### 2.1. Prérequis
- **Java 11 ou supérieur** (idéalement Java 17 ou 23, selon votre configuration).
- **Maven** ou **Gradle** pour la construction.
- **Cassandra** installé ou déployé via Docker.
- **Docker** (optionnel, pour containeriser l’application et/ou la base de données).

### 2.2. Configuration et Préparation
1. **Configurer la base de données Cassandra**  
   Assurez-vous que Cassandra est correctement installé et accessible.  
   Par exemple, pour Docker :  
   ```bash
   docker run --name cassandra -d -p 9042:9042 cassandra:latest
   ```
   Vérifiez la connexion avec :
   ```bash
   docker exec -it cassandra cqlsh
   ```
   Puis créez (si besoin) le keyspace `transportapp` :
   ```cql
   CREATE KEYSPACE IF NOT EXISTS transportapp WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
   ```

2. **Configurer l’application**  
   Dans `src/main/resources/application.properties` (ou `application.yml`), configurez Cassandra :
   ```properties
   spring.application.name=QRAPI
   spring.data.cassandra.contact-points=127.0.0.1
   spring.data.cassandra.port=9042
   spring.data.cassandra.keyspace-name=transportapp
   spring.data.cassandra.local-datacenter=datacenter1
   spring.data.cassandra.schema-action=create-if-not-exists
   server.port=8080
   ```
   Assurez-vous que le `local-datacenter` correspond bien à celui de votre instance Cassandra. Vous pouvez vérifier dans `cqlsh` avec :
   ```cql
   SELECT data_center FROM system.local;
   ```

3. **Vérifier les dépendances**  
   Dans votre `pom.xml`, assurez-vous d’inclure :
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
   <!-- Autres dépendances nécessaires, par exemple jjwt, ZXing, etc. -->
   ```

### 2.3. Construction et Lancement
1. **Construire l'application**  
   Avec Maven, exécutez :
   ```bash
   mvn clean install
   ```
2. **Lancer l'application**  
   Exécutez :
   ```bash
   java -jar target/QRAPI-0.0.1-SNAPSHOT.jar
   ```
   Votre application sera accessible sur `http://localhost:8080`.

### 2.4. Déploiement avec Docker (Optionnel)
1. **Créer un Dockerfile** dans le répertoire racine :
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

## 3. Modèle Mathématique et Modélisation de la QRAPI

### 3.1. Modèle Mathématique

#### **Entités et Ensembles**
- Soit \( C \) l'ensemble des **Chauffeurs** :
  \[
  C = \{ c \mid c = (id_c, nom, ... ) \}
  \]
- Soit \( L \) l'ensemble des **Clients** :
  \[
  L = \{ l \mid l = (id_l, nom, ... ) \}
  \]
- Soit \( R \) l'ensemble des **Courses** :
  \[
  R = \{ r \mid r = (id_r, id\_chauffeur, id\_client, lieu, heure, date, ville, pays) \}
  \]
- Soit \( Q \) l'ensemble des **QR Codes** :
  \[
  Q = \{ q \mid q = (id_q, hash, id\_course) \}
  \]
- Soit \( H \) l'ensemble des **Historique de Scans** :
  \[
  H = \{ h \mid h = (id_h, id\_course, id\_chauffeur, id\_client, timestamp) \}
  \]

#### **Relations et Fonctions**
- **Génération du QR Code**  
  Une fonction \( f: R \to Q \) prend une course et génère un QR Code unique basé sur le hash des informations de la course :
  \[
  f(r) = q \quad \text{avec} \quad hash = H(\text{json}(r))
  \]
  où \( H \) est la fonction de hachage (SHA-256).

- **Validation/Scan du QR Code**  
  Une fonction \( g: Q \to \{ \text{valide}, \text{invalide} \} \) qui, en vérifiant la signature du QR code (token JWT signé), retourne l'état de validité.  
  Si \( g(q) = \text{valide} \), alors l’historique \( h \) peut être mis à jour avec les informations associées.

---

### 3.2. Scénarios et Cas d'Utilisation

#### **Scénario 1 : Génération d'un QR Code**
- **Acteur** : Application de gestion de courses)  
- **Préconditions** :  
  - Une course est créée et enregistrée dans le système.
  - "L’application" s’est authentifiée via `/api/reserve` et possède un token JWT.
- **Flux principal** :  
  1. "L'application" envoie une requête `POST /api/qr/generate` avec les données QRData (mélange d'infos sur la course et sur la localisation spatiotemporelle du lieu du client au moment où il a voulu générer son code QR), la clé secrète de l'application, et une durée d'expiration (pour déterminer la validité du QR).
  2. Le système attribue un UUID à la course et enregistre l’information.
  3. Le système calcule un hash (SHA-256) des données de la course.
  4. Le hash est signé (avec la clé secrète et une durée d’expiration) pour générer un token.
  5. Ce token est transformé en image QR (PNG) via ZXing.
  6. Le QR code est renvoyé au client.
- **Postconditions** :  
  - Le QR code est généré et stocké (le hash et les données associées sont enregistrées dans la table `QRHash` (en réalité il y a une autre table QRData pour les données en clair ...).

#### **Scénario 2 : Scan et Validation d'un QR Code**
- **Acteur** : Chauffeur ou Client (selon le processus métier)  
- **Préconditions** :  
  - Un QR code a été généré et est en possession de l’acteur.
  - L'application sur laquelle les deux interagissent s'est authentifiée et possède un token JWT.
- **Flux principal** :  
  1. Le chauffeur scanne le QR code via une application mobile ou un lecteur.
  2. La requête `POST /api/qr/scan` est envoyée avec le contenu du QR code et les données d’historique (localisation spatiale, et temporelle), et la clé secrète de l'application.
  3. Le système décode le token contenu dans le QR code et vérifie sa signature.
  4. Si le token est valide, le système retrouve les données associées dans la table `QRData`.
  5. Le système enregistre l'opération dans la table `History`.
  6. Une réponse de succès est renvoyée.
- **Flux alternatif** :  
  - Si le QR code est invalide ou expiré, le système renvoie une réponse d’erreur (ex. : "QR Code invalide ou non trouvé").

NB : Chaque organisation utilisant l'API doit générer leur clés propre qui sera utiliser pour générer le QRCode.
---

### 3.3. Diagramme de Classes (UML Simplifié)

```
+----------------+       +----------------+       +----------------+
|    QRData      |       |    QRHash      |       |    History     |
+----------------+       +----------------+       +----------------+
| - id : UUID    |<>-----| - id : UUID    |       | - id : UUID    |
| - clientId     |       | - hash : String|       | - courseId     |
| - chauffeurId  |       | - qrDataId     |       | - clientId     |
| - courseId     |       +----------------+       | - chauffeurId  |
| - lieu         |                              | - fournisseur  |
| - heure        |                              | - timestamp    |
| - date         |                              +----------------+
| - ville        |
| - pays         |
| - fournisseur  |
+----------------+

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

---

## 4. Guide de Déploiement de la QRAPI

### 4.1. Prérequis

- **Java** (version 11 ou supérieure, idéalement Java 17 ou 23 selon la configuration)
- **Maven** ou **Gradle** pour la construction
- **Cassandra** (installé localement, en Docker, ou sur un cluster distant)
- **Docker** (optionnel, pour containeriser l’application et/ou Cassandra)

### 4.2. Configuration

#### a) Fichier `application.properties` (ou `application.yml`)
Exemple de configuration pour Cassandra dans `application.properties` :

```properties
spring.application.name=QRAPI
spring.data.cassandra.contact-points=127.0.0.1
spring.data.cassandra.port=9042
spring.data.cassandra.keyspace-name=transportapp
spring.data.cassandra.local-datacenter=datacenter1
spring.data.cassandra.schema-action=create-if-not-exists
server.port=8080
```

> **Note :** Vérifiez que le `local-datacenter` correspond bien au datacenter configuré dans Cassandra (par exemple, via `SELECT data_center FROM system.local;` dans cqlsh).

#### b) Configuration du JWT
Vous pouvez définir dans vos propriétés (ou dans un fichier de configuration séparé) la clé secrète à utiliser pour signer et vérifier les tokens. Par exemple :
```properties
jwt.secret=464313da08dcfbc5e3bf6eb5367f87ec760ff7b9699505ef2ee806296da3e5a7565c74e3d89ffdca9ab55e5144f1fb9a92c2697ab67077b11ca134a510d8d77e
```
Cette clé sera utilisée par votre classe `JwtUtil`.

### 4.3. Construction et Lancement

#### a) Construction
Avec Maven :
```bash
mvn clean install
```
Cela génère un fichier JAR dans le répertoire `target/`.

#### b) Lancement
Lancez l'application avec la commande :
```bash
java -jar target/QRAPI-0.0.1-SNAPSHOT.jar
```
L'application sera accessible sur `http://localhost:8080`.

### 4.4. Déploiement via Docker (Optionnel)
Pour containeriser l’application, créez un fichier **Dockerfile** à la racine du projet :

```dockerfile
FROM openjdk:17-jdk-alpine
COPY target/QRAPI-0.0.1-SNAPSHOT.jar /app/QRAPI.jar
WORKDIR /app
EXPOSE 8080
CMD ["java", "-jar", "QRAPI.jar"]
```

#### a) Construction de l'image Docker
```bash
docker build -t qrapi .
```

#### b) Lancement du conteneur Docker
```bash
docker run -p 8080:8080 qrapi
```

---

## 5. Scénarios et Cas d'Utilisation

### 5.1. Scénario : Authentification et Obtention du Token
- **Acteur** : Fournisseur (utilisateur de l’API)
- **Précondition** : L'utilisateur est enregistré ou identifié dans le système.
- **Flux principal** :
  1. L'utilisateur appelle l'endpoint `/api/reserve` en fournissant son identifiant (par exemple, `Fra Yuuki`).
  2. L'API génère un token JWT avec le sujet (sub) égal au nom du fournisseur.
  3. Le token est renvoyé à l'utilisateur qui le sauvegarde pour les appels suivants.
- **Postcondition** : L'utilisateur dispose d'un token JWT valide.

### 5.2. Scénario : Génération du QR Code
- **Acteur** : Fournisseur authentifié (par exemple, un chauffeur)
- **Précondition** : L'utilisateur a un token JWT valide obtenu via `/api/reserve`.
- **Flux principal** :
  1. L'utilisateur envoie une requête `POST /api/qr/generate` avec :
     - Un objet `QRData` contenant les informations de la course.
     - Les paramètres `secret` (clé de signature) et `expirationMillis` pour le token de validité.
     - Le token JWT dans l'en-tête `Authorization`.
  2. Le système attribue un UUID au QRData et l'enregistre dans la base.
  3. Le système calcule un hash des données (via SHA-256) et le signe avec la clé fournie.
  4. Le système génère un QR code (image PNG) à partir du token signé.
  5. Le QR code est renvoyé à l'utilisateur.
- **Postcondition** : Un QR code est généré, et les données associées (QRData et QRHash) sont stockées dans la base.

### 5.3. Scénario : Scan et Validation du QR Code
- **Acteur** : Fournisseur authentifié (par exemple, un chauffeur ou un agent de vérification)
- **Précondition** : Le QR code a été généré et distribué.
- **Flux principal** :
  1. L'utilisateur scanne le QR code via une application mobile ou un scanner.
  2. La requête `POST /api/qr/scan` est envoyée avec :
     - Le token signé extrait du QR code (en paramètre `qrCodeData`).
     - Le même `secret` utilisé pour la signature.
     - Un objet `History` dans le corps de la requête pour enregistrer l'opération.
     - Le token JWT dans l'en-tête `Authorization`.
  3. Le système vérifie la signature du token (via la méthode `verifySignature`).  
     - Si la signature est valide, le système récupère l'objet `QRHash` associé et, à partir de celui-ci, les données complètes de la course (`QRData`).
  4. Le système enregistre l'opération dans la table `History`.
  5. Une réponse de succès est renvoyée.
- **Flux alternatif** :
  - Si la signature est invalide ou le QR code n’est pas trouvé, le système renvoie un code d'erreur (HTTP 401 ou 404) avec un message d'erreur.
- **Postcondition** : L’opération de scan est enregistrée dans l’historique, et l'utilisateur reçoit un retour indiquant la validité du QR code.

---

## 6. Résumé des Classes et Relations

### 6.1. Principales Classes Domaines
- **QRData**  
  Représente les informations de la course (client, chauffeur, course, lieu, heure, date, etc.).  
  Attributs : `id (UUID)`, `clientId`, `chauffeurId`, `courseId`, `lieu`, `heure`, `date`, `ville`, `pays`, `fournisseur`.

- **QRHash**  
  Représente le hash signé associé à une instance de `QRData`.  
  Attributs : `id (UUID)`, `hash (String)`, `qrDataId (UUID)`.

- **History**  
  Enregistre l’historique des scans, incluant les identifiants des entités concernées.  
  Attributs : `id (UUID)`, `clientId`, `chauffeurId`, `courseId`, `fournisseur`, et éventuellement un timestamp.

### 6.2. Services et Contrôleurs
- **QRCodeController**  
  Gère les endpoints `/api/qr/generate` et `/api/qr/scan` :
  - La méthode **generateQRCode** génère le QR code à partir des données et du hash signé.
  - La méthode **scanQRCode** valide le QR code, récupère les informations, et enregistre l’opération dans l’historique.

- **ScanService**  
  Contient la logique métier de traitement d’un scan (validation du QR code, recherche dans les repositories, etc.).

- **JwtUtil**  
  Gère la signature et la validation des tokens JWT.

- **Repositories**  
  - **QRDataRepository** : Accès aux données de QRData.
  - **QRHashRepository** : Accès aux données de QRHash.
  - **HistoryRepository** : Accès à l’historique des scans.

### 6.3. Relations et Flux de Données
- **QRData** est la source des données de la course.  
- Le **QRHash** est généré à partir de l’objet QRData (via un hash de son contenu) et signé avec un token JWT.  
- Lors du scan, le QR code (contenant le token signé) est décodé et validé pour retrouver le **QRHash**, qui sert ensuite à retrouver l’objet **QRData**.  
- Le **History** enregistre l’opération de scan avec les informations provenant de QRData.

---