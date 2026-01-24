# BingoPlugin - Clash des Ecoles

## Fichiers de configuration

### config.yml
Configuration principale (API, scoreboard, tablist, timer, auto-teleport)

### messages.yml
Tous les messages du plugin externalisés

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
