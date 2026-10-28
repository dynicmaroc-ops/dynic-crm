# Application Android — Gestion des Devis & Bons de Livraison (BL) — DYNIC

> **Instruction pour Gemini (Google AI Studio)** : construis une application Android qui permet de créer, gérer et exporter en PDF des **devis** et des **bons de livraison (BL)**. Les PDF générés doivent **reproduire fidèlement** le style du modèle de devis DYNIC décrit ci-dessous (en-tête, filigrane, bloc client, tableau, totaux, cachet, pied de page). Les mêmes en-tête et pied de page servent pour les devis et les BL.

## 0. Fichiers à joindre au prompt (dossier `assets/`)

| Fichier | Rôle |
|---|---|
| `dynic_fond_page.png` | Fond A4 complet (1592×2288 px, PNG transparent) : en-tête + logo + slogan, filigrane « Dynic », pied de page |
| `dynic_entete.png` | En-tête seul (bandeau vert, logo, slogan) |
| `dynic_pied_de_page.png` | Pied de page seul (bloc vert + coordonnées) |
| `dynic_cachet.png` | Cachet rond bleu « DYNIC sarl » avec signature (PNG transparent) |
| `Devis_TP-27-0101_26-10-26.pdf` | **PDF de référence visuelle** : le rendu final attendu |

> Utiliser `dynic_fond_page.png` tel quel comme fond de chaque page du PDF. Ne pas redessiner le logo. Les textes dynamiques (date, client, numéro, tableau, totaux) sont dessinés par-dessus.

---

## 1. Contexte

- Entreprise : **DYNIC sarl** (publicité, impression, signalétique, emballage), Salé, Maroc. Slogan : « Dynamisé votre succès avec notre expertise ! ».
- Langue : **français**. Devise : **Dirham marocain (DH)**. TVA par défaut : **20 %**.
- Usage mobile, **hors-ligne**, export et partage PDF (WhatsApp, e-mail, impression).

## 2. Stack technique recommandée

- **Kotlin + Jetpack Compose** (Material 3), architecture MVVM (ViewModel + StateFlow)
- **Room** (SQLite), offline-first, couche Repository (synchronisation future possible)
- **PDF** : `android.graphics.pdf.PdfDocument` + Canvas (A4 = 595×842 pt)
- **Partage** : `FileProvider` + `Intent.ACTION_SEND`
- Navigation Compose

## 3. Charte graphique

| Élément | Valeur |
|---|---|
| Vert principal (bandeau, en-têtes de tableau, libellés des totaux) | `#1DB094` |
| Gris clair (cellules UM/Qté/PU/Total, valeurs des totaux) | `#F2F2F2` |
| Texte | `#000000` |
| En-têtes de tableau | texte **blanc, gras**, centré |
| Police | **Calibri** (Carlito si indisponible), 11 pt pour le corps |
| Cachet | bleu (image fournie) |

Thème de l'appli : Material 3, `primary = #1DB094`, coins arrondis, fond clair.

---

## 4. Design du PDF — Devis (A4 portrait, 595×842 pt, origine en haut à gauche)

### 4.1 Fond de page (toutes les pages)
Image `dynic_fond_page.png` étirée sur toute la page. Elle contient :
- **En-tête** : petit carré vert à gauche, **logo « Dynic »** vert, slogan en dessous, **longue barre verte** jusqu'au bord droit.
- **Filigrane** « Dynic » géant, vert très pâle, au centre de la page, derrière le contenu.
- **Pied de page** : bloc vert arrondi à gauche + `DYNIC Sarl` et 3 lignes de coordonnées (Adresse, Fixe, Mobile, Capital Social, RC, Patente, IF, ICE, Banque, Mail, Web).

> Si le pied de page doit être modifiable depuis les Paramètres, le redessiner en texte (Calibri 8 pt, libellés en gras) à partir des champs société, à la place de l'image ; zone réservée : de y ≈ 765 pt jusqu'au bas de page. **Ne jamais placer de contenu dans cette zone.**

### 4.2 Date
- Texte `Salé le : JJ/MM/AAAA`, Calibri 11 pt, **aligné à gauche à x = 373 pt**, ligne de base à y ≈ 99 pt.

### 4.3 Bloc client
- Trois lignes à **x = 243 pt**, interligne ≈ 14,3 pt, première ligne de base à y ≈ 160 pt.
- Libellé en **gras**, valeur en normal :
  ```
  Client : <nom>
  Adresse : <adresse>
  ICE : <ICE>
  ```

### 4.4 Titre
- `Devis N° :  <numéro>` en **gras 14 pt**, à **x = 34 pt**, ligne de base à y ≈ 231 pt.

### 4.5 Tableau des lignes
- Position : **x = 32 → 522 pt**, haut du tableau à **y ≈ 274 pt**.
- **En-tête** (hauteur ≈ 22 pt) : fond `#1DB094`, texte blanc gras centré, bordures noires fines.
- Colonnes et largeurs :

| Colonne | Largeur (pt) | Alignement | Fond des cellules |
|---|---|---|---|
| Désignation | 221 | gauche | blanc |
| UM | 58 | centré | `#F2F2F2` |
| Qté | 60 | centré | `#F2F2F2` |
| PU (HT) | 70 | droite | `#F2F2F2` |
| Total (HT) | 81 | droite | `#F2F2F2` |

- Bordures noires de 0,6 pt ; bordure gauche et bas de la colonne Désignation un peu plus épaisses (≈ 1,4 pt).
- Hauteur de ligne minimale ≈ 22 pt, padding vertical ≈ 4,5 pt ; la hauteur grandit si la désignation passe sur plusieurs lignes.
- **Désignation** : texte simple (pas de gras ni de souligné), possibilité de plusieurs lignes de détail.
- **Formats** : milliers séparés par un espace, virgule décimale, toujours 2 décimales (`12 300,00`). UM courantes : `U`, `M²`, `ML`, `Forfait`.
- Si le tableau dépasse la page : saut de page, **l'en-tête du tableau est répété**, le fond de page aussi.

### 4.6 Bloc des totaux
- Sous le tableau (espace ≈ 14 pt), **aligné à droite** (x = 371 → 522 pt), 3 lignes de ≈ 17 pt :

| Libellé (fond vert, blanc gras, centré, 70 pt) | Valeur (fond gris, droite, 81 pt) |
|---|---|
| `Total Ht` | total HT |
| `TVA  20%` | montant TVA |
| `TTC` | total TTC |

- Le taux de TVA affiché suit le paramètre du devis.

### 4.7 Cachet
- Image `dynic_cachet.png` sous les totaux (espace ≈ 6 pt), environ **112 × 130 pt**, centrée vers x ≈ 390 pt.
- Interrupteur « Apposer le cachet » (activé par défaut) à l'export.
- Les totaux et le cachet restent ensemble (jamais séparés par un saut de page).

---

## 5. Design du PDF — Bon de livraison (BL)

Même fond de page, même date, même bloc client, même style de tableau et de cachet. Différences :
- Titre : `Bon de livraison N° :  TD-26-00027`
- Colonnes : `Réf | Désignation | UM | Quantités livrées` (pas de prix ni de totaux).
- Désignation : première ligne en **gras souligné** (article + dimensions), puis lignes de détail avec tirets (`- Papier : …`).
- Zone optionnelle « Reçu par / Signature client » à côté du cachet.

---

## 6. Modèle de données (Room)

```kotlin
Company(id, nom, slogan, adresse, fixe, mobile1, mobile2, capital, rc, patente, ifiscal, ice,
        banqueRib, banqueAgence, mail, web, ville, logoUri, cachetUri, fondPageUri)

Client(id, nom, adresse, ice, telephone, email)

Article(id, ref, designation, details: List<String>, um, prixUnitaireHT)

Document(id, type /* DEVIS | BL */, numero, date, clientId,
         statut /* BROUILLON | VALIDE | ENVOYE | ACCEPTE | LIVRE | ANNULE */,
         sourceDocumentId?, tvaTaux, notes, avecCachet)

LigneDocument(id, documentId, articleId?, ref, designation, details: List<String>, um, quantite, prixUnitaireHT)

Compteur(type, prefixe, serie, dernierNumero)
```

### Numérotation
- Devis (modèle actuel) : `TP-27-0100` → `<PREFIXE>-<SÉRIE>-<NNNN>`
- BL (modèle actuel) : `TD-26-00027` → `<PREFIXE>-<SÉRIE>-<NNNNN>`
- Préfixe, série, longueur et dernier numéro **configurables** dans les Paramètres ; incrément automatique à chaque nouveau document.

## 7. Écrans

1. **Accueil** : cartes (devis du mois, BL du mois, en attente), boutons **+ Devis** / **+ BL**, derniers documents.
2. **Liste des documents** : onglets Devis / BL, recherche, filtres statut et date.
3. **Création / édition** : choix du client (recherche + création rapide), date, numéro auto, lignes (depuis le catalogue ou saisie libre), totaux en temps réel, bouton **Aperçu PDF**.
4. **Aperçu PDF** fidèle A4 : zoom, Partager, Imprimer, Enregistrer.
5. **Conversion** : « Transformer le devis en BL » (quantités modifiables pour livraison partielle), « Dupliquer ».
6. **Clients** : liste, fiche, historique.
7. **Catalogue articles** : références, désignation, unité, prix.
8. **Paramètres** : infos société, logo/fond/cachet, numérotation, TVA par défaut, sauvegarde/restauration (JSON/ZIP).

## 8. Règles fonctionnelles

- Un document validé n'est plus modifiable sauf action « Réouvrir ».
- Calculs : `Total ligne = Qté × PU`, `TVA = Total HT × taux`, arrondi à 2 décimales.
- Nom du PDF : `Devis_<Numéro>_<JJ-MM-AA>.pdf` / `BL_<Numéro>_<JJ-MM-AA>.pdf`.
- Interface en français ; dates `JJ/MM/AAAA` ; nombres `12 300,00`.
- Contrôle orthographique léger sur les désignations (accents, « Trophée », « Découpe », « Boîte »…).

## 9. Données de démonstration (devis exemple, client à saisir)

| Désignation | UM | Qté | PU (HT) | Total (HT) |
|---|---|---|---|---|
| Minaret Koutoubia | U | 1,00 | 2 500,00 | 2 500,00 |
| Numéros en Forex, découpe + peinture | U | 1,00 | 500,00 | 500,00 |
| Découpe laser lettres | U | 1,00 | 0,00 | 0,00 |
| Trophée en plexiglas 10 mm + support bois | U | 18,00 | 240,00 | 4 320,00 |
| Boîte en plexiglas « Octobre Rose » | U | 10,00 | 170,00 | 1 700,00 |
| Découpe Forex 150x150 | U | 9,00 | 300,00 | 2 700,00 |
| Découpe plexiglas | U | 3,00 | 300,00 | 900,00 |
| Découpe carton plume | U | 17,00 | 50,00 | 850,00 |
| Forex 5 mm 1/4 + découpe | U | 1,00 | 200,00 | 200,00 |
| Trophée en Aluco + Isorel | U | 90,00 | 17,00 | 1 530,00 |
| Badge ABS | U | 62,00 | 6,00 | 372,00 |

**Total HT : 15 572,00 — TVA 20 % : 3 114,40 — TTC : 18 686,40 DH.**
Ce jeu de données sert de test : le PDF généré doit ressembler à `Devis_TP-27-0101_26-10-26.pdf`.

## 10. Critères d'acceptation

- [ ] Le PDF d'un devis est visuellement identique au PDF de référence (fond, date, client, titre, tableau gris/vert, totaux, cachet, pied de page).
- [ ] Le filigrane reste derrière le contenu et ne gêne pas la lecture.
- [ ] Le contenu ne déborde jamais sur le pied de page ; pagination correcte avec en-tête de tableau répété.
- [ ] Les calculs HT / TVA / TTC sont exacts (jeu de test ci-dessus).
- [ ] Devis et BL partagent le même en-tête et le même pied de page.
- [ ] Fonctionne hors-ligne ; partage PDF vers WhatsApp en 2 appuis.

## 11. Livrables demandés à Gemini

1. Projet Android Studio complet (Kotlin, Compose, Room).
2. Classe `PdfGenerator` conforme aux sections 4 et 5 (positions en points, fond `dynic_fond_page.png`).
3. Données de démonstration (section 9) et README d'installation.
