# Architecture et Infrastructure de PresidentGame

## Vue d'ensemble

PresidentGame est une application de jeu de cartes en ligne basée sur le jeu Président. Le backend, développé avec Spring Boot (Java 17), gère la logique métier, les règles du jeu et les actions des joueurs. Le frontend est développé en React et interagit avec le backend via une API REST et une connexion WebSocket pour la synchronisation en temps réel.

## Composants Principaux

### 1. API REST (Backend)
- **Technologies :** Java 17, Spring Boot
- **Fonctions :**
    - Gestion de la logique métier (création et gestion des parties, règles du jeu, actions comme jouer des cartes, passer, etc.)
    - Exposition d'endpoints sécurisés pour l'authentification, l'inscription, et les interactions liées aux parties.
- **Sécurité :**
    - Authentification basée sur JWT et gestion des rôles avec Spring Security.

### 2. WebSocket Server
- **Technologies :** Spring WebSocket
- **Fonctions :**
    - Communication bidirectionnelle en temps réel pour synchroniser l'état du jeu entre les clients.
    - Diffusion des mises à jour instantanées (cartes jouées, changements de tour, notifications, etc.).

### 3. Base de Données
- **Technologies :** PostgreSQL
- **Fonctions :**
    - Stockage persistant des états des parties, des utilisateurs, des scores et des historiques de jeu.
    - Interaction via Spring Data JPA pour gérer les transactions et la persistance.

### 4. Frontend
- **Technologies :** React (avec une préférence pour TypeScript)
- **Fonctions :**
    - Interface utilisateur responsive et fluide.
    - Affichage de l'état du jeu, gestion des interactions utilisateur, et intégration d'un chat en temps réel.

## Flux de Communication

1. **API REST :**
    - Le frontend communique avec le backend en envoyant des requêtes HTTP (pour l'authentification, la gestion des parties, etc.) et reçoit des réponses au format JSON.

2. **WebSocket :**
    - Une connexion WebSocket est établie pour permettre une communication instantanée entre le backend et le frontend.
    - Le backend diffuse en temps réel l'état du jeu (actions des joueurs, changements de tour, etc.) aux clients connectés.

3. **Base de Données :**
    - Toutes les transactions liées aux parties et aux utilisateurs sont enregistrées dans PostgreSQL, garantissant la persistance des données.

## Déploiement et Hébergement

- **Conteneurisation :**
    - L'application backend (et éventuellement le frontend) peut être conteneurisée à l'aide de Docker pour faciliter le déploiement.
- **Orchestration :**
    - Pour un déploiement en production, Kubernetes est envisagé afin de gérer la scalabilité, le load balancing et la résilience des services.
- **Sécurité :**
    - Utilisation de certificats SSL pour sécuriser les connexions (HTTPS).
    - Mise en place d'une surveillance centralisée (logs, monitoring des performances) pour garantir la stabilité de l'application.

## Diagramme d'Architecture

Un diagramme d'architecture détaillé peut aider à visualiser l'interaction entre les composants. Ce diagramme devrait inclure :
- L'API REST (Spring Boot)
- Le serveur WebSocket (Spring WebSocket)
- La base de données PostgreSQL
- Le frontend (React)

*Astuce : Vous pouvez créer ce diagramme à l'aide d'outils tels que Draw.io, Lucidchart ou PlantUML et l'inclure sous forme d'image dans ce document.*

## Notes Techniques et Sécurité

- **Authentification et Autorisation :**
    - L'application utilise JWT pour sécuriser les endpoints.
    - Les rôles et permissions sont gérés via Spring Security.
- **Communication en Temps Réel :**
    - Les connexions WebSocket permettent une synchronisation efficace des mises à jour du jeu entre tous les clients.
- **Performance et Scalabilité :**
    - L'architecture conteneurisée (Docker) et l'orchestration avec Kubernetes (pour le déploiement futur) visent à garantir une haute disponibilité et une scalabilité horizontale.
- **Monitoring :**
    - Prévoir des outils de monitoring et de log pour observer la santé du système et réagir rapidement aux incidents.

## Conclusion

Ce document présente une vue d'ensemble de l'architecture de PresidentGame. Il pourra être enrichi et mis à jour au fur et à mesure de l'évolution du projet, notamment lorsque l'application passera en phase de déploiement sur serveur.
