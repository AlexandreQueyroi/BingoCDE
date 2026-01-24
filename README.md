# BingoPlugin - Clash des Ecoles

Plugin Minecraft Spigot refactorise selon vos specifications.

## Structure

- 1 fichier = 1 commande/event/manager
- Messages externalises dans messages.yml
- Code non commente
- Pas d'emojis
- Support des icones d'items via default.json
- Teleportation automatique lors de la creation de partie
- Commande /bingo reload pour recharger la config

## Fichiers de configuration

### config.yml
Configuration principale (API, scoreboard, tablist, timer, auto-teleport)

### messages.yml
Tous les messages du plugin externalisés

### item_icons.properties
Mapping des items Minecraft vers les caracteres Unicode pour affichage dans le scoreboard

## Commandes

### Equipes
- `/bingo team create <nom> <couleur>` - Creer une equipe
- `/bingo team add <equipe> <joueur>` - Ajouter un joueur
- `/bingo team remove <joueur>` - Retirer un joueur
- `/bingo team chat <equipe> <message>` - Message d'equipe

### Parties
- `/bingo instance create <equipe1> <equipe2>` - Creer une partie (teleportation auto)
- `/bingo instance match set <nom>` - Nommer le match
- `/bingo roll` - Tirer 25 objectifs
- `/bingo start/pause/stop` - Gerer le chronometre

### Objectifs
- `/bingo valid <id> <equipe>` - Valider un objectif
- `/bingo reject <id> <equipe>` - Rejeter un objectif

### Affichage
- `/bingo show` - Afficher le scoreboard
- `/bingo hide` - Cacher le scoreboard

### Teleportation
- `/bingo tp <joueur>` - Se teleporter vers un joueur (admin)
- `/bingo tphere <joueur>` - Teleporter un joueur vers soi (admin)

### Administration
- `/bingo export <idPartie>` - Exporter les resultats
- `/bingo say <message>` - Message global
- `/bingo reload` - Recharger la configuration

## Chat special

- Message normal : chat d'equipe
- `. message` : chat global
- `! message` : chat admin (reserve aux admins)

## Compilation

```bash
mvn clean package
```

## Installation

1. Compiler le plugin
2. Copier le JAR dans plugins/
3. Configurer config.yml et messages.yml
4. Placer le resource pack avec default.json pour les icones
5. Redemarrer le serveur

## Routes API

Le plugin utilise toutes les routes API fournies.

## Differences avec la version precedente

- Code non commente
- Messages externalises
- 1 fichier par commande/event/manager
- Pas d'emojis
- Support des icones d'items
- Teleportation auto activable
- Commande reload
- Commandes tp modifiees (admin vers joueur / joueur vers admin)
