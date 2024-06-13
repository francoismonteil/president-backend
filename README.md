# PresidentGame Backend

Bienvenue dans le backend de PresidentGame, une application de jeu de cartes en ligne basée sur le jeu de cartes Président. Ce projet utilise Spring Boot pour le backend et React pour le frontend.

## Prérequis

- Java 17 ou supérieur
- Maven
- PostgreSQL

## Installation

1. Clonez le dépôt

```sh
git clone https://github.com/votre-utilisateur/presidentgame-backend.git
cd presidentgame-backend
```

2. Configurez la base de données PostgreSQL

Créez une base de données PostgreSQL et mettez à jour les informations de connexion dans src/main/resources/application.properties :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/nom_de_votre_base
spring.datasource.username=votre_nom_utilisateur
spring.datasource.password=votre_mot_de_passe
```

3. Construisez le projet avec Maven

```shell
   mvn clean install
```

4. Démarrez l'application

```shell
  mvn spring-boot:run
```