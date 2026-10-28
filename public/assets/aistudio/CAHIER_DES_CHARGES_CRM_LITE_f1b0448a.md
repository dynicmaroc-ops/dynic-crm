# Cahier des charges — DYNIC CRM Lite (application Android)

> **Instruction pour Gemini (Google AI Studio)** : ce document est la spécification complète de l'application. Génère un projet Android Studio (Kotlin + Jetpack Compose) conforme à ce cahier des charges, en respectant l'arborescence (section 12), la navigation (section 11) et le style des documents PDF (section 8). Les modèles de documents (devis, BL, facture, bon de commande) et les assets graphiques sont fournis en pièces jointes.

**Version** : 1.0 — 27/10/2026
**Éditeur / client pilote** : DYNIC sarl, Salé (Maroc)
**Langue** : français — **Devise** : Dirham marocain (DH) — **TVA par défaut** : 20 %

---

## 1. Présentation et objectifs

### 1.1 Principe
Un **CRM lite** pour les petites entreprises (publicité, impression, signalétique, négoce) qui regroupe l'essentiel de la gestion quotidienne dans une seule application mobile :

- fichier **clients** et **fournisseurs**
- catalogue **produits / services**
- **devis → bon de livraison (BL) → facture**, et **bons de commande** fournisseurs
- **dépenses** et paiements
- **scan par IA** : une photo de facture alimente les dépenses ; une photo de BL fournisseur alimente produits et fournisseurs
- connexion par **compte Google**, données synchronisées avec **Firebase**, utilisable hors-ligne

### 1.2 Objectifs
1. Créer un devis, un BL ou une facture en moins de 2 minutes depuis le téléphone.
2. Supprimer la saisie manuelle des achats grâce au scan (facture → dépense, BL → produits + fournisseur).
3. Produire des PDF **identiques au style DYNIC** (en-tête, filigrane, tableau, cachet, pied de page).
4. Données sécurisées, sauvegardées dans le cloud, accessibles sur plusieurs appareils.

### 1.3 Hors périmètre (version 1)
Comptabilité complète (journaux, bilan), paie, e-commerce, application iOS, facturation électronique réglementaire (à étudier plus tard).

---

## 2. Utilisateurs et rôles

| Rôle | Droits |
|---|---|
| **Propriétaire** | Tout, y compris paramètres société, utilisateurs, suppression, export |
| **Gestionnaire** | Créer / modifier documents, clients, produits, dépenses ; pas de paramètres société |
| **Commercial** | Clients, devis, BL ; lecture des produits ; pas d'accès aux dépenses ni aux marges |
| **Lecture seule** | Consultation uniquement |

Une **société (workspace)** peut avoir plusieurs utilisateurs invités par e-mail Google.

---

## 3. Authentification et méthode de connexion

### 3.1 Principe
- **Connexion unique : « Se connecter avec Google »** via **Credential Manager (Google ID)** → échange du jeton Google contre un identifiant **Firebase Authentication** (`GoogleAuthProvider.getCredential(idToken)`).
- Pas de mot de passe géré par l'application.
- Session persistante (l'utilisateur reste connecté), déconnexion manuelle possible.

### 3.2 Parcours
1. **Écran d'accueil / Splash** : logo DYNIC, vérification de la session.
2. **Écran de connexion** : bouton « Continuer avec Google » (sélecteur de comptes Android).
3. **Premier lancement** :
   - nouvel utilisateur sans société → **Assistant de création de société** (nom, ICE, IF, RC, Patente, adresse, téléphones, logo, cachet, TVA, préfixes de numérotation)
   - utilisateur invité → rattachement automatique à la société qui l'a invité
4. **Choix de société** si l'utilisateur appartient à plusieurs.
5. **Verrouillage optionnel** de l'application par biométrie / code de l'appareil (`BiometricPrompt`).

### 3.3 Invitations et rôles
- Le propriétaire saisit l'e-mail Google d'un collaborateur et choisit son rôle.
- À la première connexion de cet e-mail, l'utilisateur est rattaché à la société.
- Le rôle est stocké dans `companies/{companyId}/members/{uid}` et contrôlé par les **règles de sécurité Firestore** (jamais seulement côté application).

### 3.4 Sécurité
- **Firebase App Check** (Play Integrity) activé pour Firestore, Storage et l'IA.
- Règles Firestore et Storage **par société et par rôle** (section 10.3).
- Aucune clé secrète dans le code de l'application.
- Données hébergées dans une région Firebase proche (Europe), conformité à la **loi marocaine 09-08** (données personnelles) : mention d'information, droit d'export et de suppression.

---

## 4. Modules fonctionnels

### 4.1 Tableau de bord
- Cartes : chiffre d'affaires du mois, factures impayées (montant + nombre), dépenses du mois, devis en attente, BL non facturés.
- Graphique recettes / dépenses sur 6 mois.
- Alertes : factures en retard, produits sous seuil de stock (si stock activé), scans à valider.
- Raccourcis : **+ Devis**, **+ BL**, **+ Facture**, **Scanner**.

### 4.2 Clients
- Fiche : nom, ICE, adresse, ville, téléphone, e-mail, contact, conditions de paiement, notes.
- Historique : devis, BL, factures, paiements, solde dû.
- Actions : appeler, WhatsApp, e-mail, créer un document.

### 4.3 Fournisseurs
- Fiche : nom, ICE, adresse, contacts, délais et conditions de paiement.
- Historique : bons de commande, BL reçus, factures d'achat / dépenses, produits fournis.
- Alimentée **automatiquement** par le scan des BL et factures (section 6).

### 4.4 Produits et services (catalogue)
- Champs : référence, désignation (titre), détails (lignes à tirets), unité (U, M², ML, kg, forfait…), **prix d'achat HT**, **prix de vente HT**, TVA, catégorie, fournisseur principal, photo, seuil d'alerte, actif/inactif.
- **Stock optionnel** (interrupteur dans les paramètres) : quantité calculée par mouvements (entrées = BL fournisseur / réception, sorties = BL client).
- Marge calculée automatiquement (visible selon le rôle).
- Import / export CSV.
- Historique des prix d'achat par fournisseur.

### 4.5 Documents de vente et d'achat

| Document | Rôle | Préfixe par défaut (exemple) |
|---|---|---|
| **Devis** | Offre de prix au client | `TP-27-0100` |
| **Bon de livraison (BL)** | Livraison au client, sans prix imposé | `TD-26-00027` |
| **Facture** | Facturation client | à définir avec le modèle joint |
| **Bon de commande** | Commande envoyée à un fournisseur | à définir avec le modèle joint |

> Les numérotations sont **configurables** (préfixe, série, longueur, prochain numéro). Les exemples viennent des modèles actuels de DYNIC.

**Chaîne de vente** : Devis → (accepté) → BL (livraison totale ou partielle) → Facture (à partir d'un ou plusieurs BL, ou directe).
**Chaîne d'achat** : Bon de commande fournisseur → réception (scan du BL fournisseur) → facture fournisseur (scan) → dépense.

Fonctions communes :
- création rapide, duplication, conversion d'un document en un autre (les lignes sont copiées, quantités modifiables)
- lignes depuis le catalogue ou en saisie libre, remises ligne et globale, TVA par ligne
- statuts : *Brouillon, Validé, Envoyé, Accepté / Refusé (devis), Livré (BL), Payé partiellement, Payé, En retard, Annulé*
- verrouillage après validation (réouverture réservée au propriétaire et au gestionnaire)
- aperçu PDF fidèle, partage WhatsApp / e-mail / impression, enregistrement dans Firebase Storage
- montant en toutes lettres (facture, si le modèle joint le prévoit)

### 4.6 Paiements et suivi des factures
- Enregistrer un paiement (date, montant, mode : espèces, virement, chèque, effet, carte), pièce jointe optionnelle.
- Paiements partiels, solde restant, relance (message WhatsApp / e-mail pré-rempli).

### 4.7 Dépenses
- Champs : date, fournisseur, catégorie (achats matière, transport, loyer, salaires, énergie, entretien, divers…), montant HT, TVA, TTC, mode de paiement, statut (payée / à payer), pièce jointe (photo ou PDF), notes.
- Création **manuelle** ou **par scan** de la facture fournisseur (section 6).
- Filtres par période, catégorie, fournisseur ; total par catégorie.

### 4.8 Rapports et exports
- Ventes par client / produit / mois, dépenses par catégorie, créances clients, marge.
- Export **CSV / Excel** et **PDF** ; export des factures d'une période pour le comptable.

### 4.9 Paramètres
- Société (identité légale, logo, cachet, fond de page), coordonnées bancaires, mentions légales.
- Numérotation, TVA, unités, catégories de dépenses, conditions de paiement par défaut.
- Utilisateurs et rôles, sécurité (biométrie), langue, thème.
- Sauvegarde / export complet (JSON / ZIP), suppression du compte.

---

## 5. Gestion hors-ligne et synchronisation

- **Firestore avec persistance hors-ligne** activée : lecture et écriture sans connexion, synchronisation automatique au retour du réseau.
- Les scans pris hors-ligne sont **mis en file d'attente** (WorkManager) puis traités par l'IA dès que le réseau revient.
- Résolution des conflits : *dernier écrit gagne* par champ, sauf documents validés (verrouillés).
- Numérotation : réservation du numéro à la **validation** via transaction Firestore pour éviter les doublons entre appareils.

---

## 6. Scan intelligent (IA)

### 6.1 Technologies
- **Capture** : *ML Kit Document Scanner* (détection de bords, recadrage, correction de perspective, multi-pages, export JPEG/PDF).
- **Extraction** : modèle **Gemini (famille Flash, version stable la plus récente)** appelé via **Firebase AI Logic** (SDK Firebase pour Android, protégé par App Check). Sortie **JSON structurée** (schéma imposé).
- Le fichier scanné est conservé dans **Firebase Storage** et lié à l'enregistrement créé.

### 6.2 Scénario A — Scanner une facture fournisseur → Dépense
1. Bouton **Scanner** → choix « Facture / Ticket ».
2. Capture de la facture (1 à N pages).
3. L'IA extrait : fournisseur (nom, ICE), n° de facture, date, échéance, lignes (facultatif), total HT, TVA, TTC, mode de paiement si indiqué.
4. **Écran de validation** : champs pré-remplis, indicateur de confiance par champ (champs douteux surlignés), catégorie suggérée, image de la facture à côté.
5. Contrôles automatiques : `HT + TVA = TTC`, ICE valide (15 chiffres), **détection de doublon** (même fournisseur + même n° + même montant).
6. L'utilisateur corrige si besoin puis **Enregistre** → création de la dépense (+ du fournisseur s'il n'existe pas) avec la pièce jointe.

### 6.3 Scénario B — Scanner un BL fournisseur → Produits + Fournisseur
1. Bouton **Scanner** → choix « Bon de livraison ».
2. L'IA extrait : fournisseur (nom, ICE, adresse), n° de BL, date, lignes (référence, désignation, unité, quantité, prix si présent).
3. **Rapprochement** de chaque ligne avec le catalogue : par référence, puis par similarité de désignation. Pour chaque ligne l'utilisateur choisit : **Lier à un produit existant**, **Créer un nouveau produit**, ou **Ignorer**.
4. Le fournisseur est lié ou créé (recherche par ICE, puis par nom).
5. À l'enregistrement : création du BL d'achat (réception), création/mise à jour des produits (fournisseur principal, prix d'achat si fourni, historique de prix), **entrée de stock** si le stock est activé, éventuelle association à un bon de commande ouvert.

### 6.4 Schéma JSON attendu de l'IA (exemple)

```json
{
  "type": "FACTURE | BL",
  "fournisseur": { "nom": "", "ice": "", "adresse": "", "telephone": "" },
  "numero": "",
  "date": "AAAA-MM-JJ",
  "echeance": "AAAA-MM-JJ",
  "lignes": [
    { "reference": "", "designation": "", "unite": "", "quantite": 0, "prix_unitaire_ht": 0, "total_ht": 0 }
  ],
  "total_ht": 0, "tva_taux": 20, "tva": 0, "total_ttc": 0,
  "confiance": { "fournisseur": 0.0, "numero": 0.0, "date": 0.0, "montants": 0.0, "lignes": 0.0 }
}
```

### 6.5 Règles
- **Aucune écriture automatique sans validation** de l'utilisateur.
- Le prompt système impose : réponse en JSON uniquement, `null` si une valeur est illisible (ne jamais inventer), montants en nombres, dates ISO.
- En cas d'échec ou de faible confiance : saisie manuelle assistée, la pièce reste attachée.
- Journal des scans (statut : en attente, à valider, validé, rejeté) visible dans **Scans**.
- Limite de taille d'image (compression avant envoi) et quota par société pour maîtriser les coûts.

---

## 7. Règles de gestion

- `Total ligne HT = Quantité × PU HT − remise` ; `TVA = Total HT × taux` ; arrondi à 2 décimales ; format `12 300,00`.
- Un devis accepté peut être transformé en BL ; un BL peut être facturé **une seule fois** (statut « Facturé »).
- Une facture payée ne peut pas être supprimée, seulement annulée par avoir (à prévoir en v2).
- Suppression logique (champ `deletedAt`) pour tous les enregistrements ; corbeille 30 jours.
- Les mentions légales du pied de page (ICE, IF, RC, Patente, capital, banque…) viennent des paramètres de la société.

---

## 8. Modèles de documents PDF

### 8.1 Principe
Un seul **moteur de rendu PDF** (`PdfEngine`) avec un **modèle par type de document**. Les modèles joints par DYNIC (devis, BL, facture, bon de commande) définissent la mise en page exacte ; en cas de différence avec ce cahier des charges, **le modèle joint fait foi**.

### 8.2 Éléments communs (tous les documents, A4 portrait 595×842 pt)
- **Fond de page** : `dynic_fond_page.png` (en-tête vert avec logo et slogan, filigrane « Dynic », pied de page avec coordonnées) étiré sur toute la page, sur chaque page.
- **Date** : `Salé le : JJ/MM/AAAA`, Calibri 11 pt, à x = 373 pt, y ≈ 99 pt.
- **Bloc client / fournisseur** : x = 243 pt, trois lignes (Client, Adresse, ICE), libellés en gras, interligne ≈ 14,3 pt, à partir de y ≈ 160 pt.
- **Titre** : `<Type> N° : <numéro>` en gras 14 pt, x = 34 pt, y ≈ 231 pt.
- **Tableau** : x = 32 → 522 pt, départ y ≈ 274 pt ; en-tête vert `#1DB094`, texte blanc gras centré ; cellules UM/Qté/PU/Total en gris `#F2F2F2` ; bordures noires 0,6 pt ; en-tête répété à chaque page.
- **Cachet** : `dynic_cachet.png`, ≈ 112 × 130 pt, sous les totaux, activable / désactivable.
- **Zone interdite** : pas de contenu à partir de y ≈ 765 pt (pied de page).
- Police **Calibri** (Carlito en secours) ; nombres `12 300,00`.

### 8.3 Spécificités par document

| Document | Titre | Colonnes du tableau | Bloc de fin |
|---|---|---|---|
| Devis | `Devis N° :` | Désignation, UM, Qté, PU (HT), Total (HT) | Totaux : `Total Ht`, `TVA 20%`, `TTC` (libellés verts, valeurs grises) |
| BL | `Bon de livraison N° :` | Réf, Désignation, UM, Quantités livrées | Cachet + zone « Reçu par / Signature » |
| Facture | `Facture N° :` | comme le devis + mentions de paiement | Totaux, montant en lettres, conditions et coordonnées bancaires (selon modèle joint) |
| Bon de commande | `Bon de commande N° :` | Réf, Désignation, UM, Qté, PU (HT), Total (HT) | Totaux, délai et lieu de livraison (selon modèle joint) |

### 8.4 Fichiers fournis (dossier `assets/`)
`dynic_fond_page.png`, `dynic_entete.png`, `dynic_pied_de_page.png`, `dynic_cachet.png` + PDF de référence `Devis_TP-27-0101_26-10-26.pdf` + modèles joints (BL, facture, bon de commande).

---

## 9. Architecture technique

| Domaine | Choix |
|---|---|
| Langage / UI | Kotlin, **Jetpack Compose**, Material 3 |
| Architecture | **MVVM + Clean Architecture** (couches `ui`, `domain`, `data`) |
| Injection | **Hilt** |
| Asynchrone | Coroutines + Flow |
| Navigation | Navigation Compose (graphe typé) |
| Base de données | **Cloud Firestore** avec persistance hors-ligne (pas de Room dans la v1) |
| Fichiers | **Firebase Storage** (scans, PDF, logos) |
| Authentification | **Firebase Auth** + Credential Manager (Google) |
| IA | **Firebase AI Logic** (Gemini Flash) + **App Check** |
| Scan | **ML Kit Document Scanner** |
| PDF | `PdfDocument` + Canvas (moteur maison, section 8) |
| Tâches différées | WorkManager (scans en attente, envoi de relances) |
| Notifications | Firebase Cloud Messaging (échéances, scans prêts) |
| Qualité | JUnit, MockK, Compose UI tests, Firebase Emulator Suite, Crashlytics |
| Min SDK | Android 8.0 (API 26) ou plus |

---

## 10. Modèle de données Firestore

### 10.1 Collections (multi-société)

```
users/{uid}                                  profil, sociétés d'appartenance
companies/{companyId}                        identité légale, paramètres, numérotation, logo/cachet
companies/{companyId}/members/{uid}          rôle, e-mail, statut d'invitation
companies/{companyId}/clients/{id}
companies/{companyId}/suppliers/{id}
companies/{companyId}/products/{id}          + sous-collection priceHistory/{id}
companies/{companyId}/stockMovements/{id}    produit, quantité, source (BL/réception), date
companies/{companyId}/documents/{id}         type: QUOTE | DELIVERY | INVOICE | PURCHASE_ORDER | SUPPLIER_DELIVERY
companies/{companyId}/documents/{id}/lines/{lineId}
companies/{companyId}/payments/{id}          facture liée, montant, mode, date
companies/{companyId}/expenses/{id}          fournisseur, catégorie, HT, TVA, TTC, pièce jointe
companies/{companyId}/scans/{id}             statut, type, fichiers, JSON extrait, confiance, résultat lié
companies/{companyId}/counters/{type}        numérotation (transaction)
companies/{companyId}/auditLog/{id}          qui a fait quoi et quand
```

### 10.2 Champs clés d'un document

```
type, number, date, dueDate?, status, clientId | supplierId, sourceDocumentId?,
currency, vatRate, discount, totalHT, totalVAT, totalTTC, paidAmount,
notes, withStamp, pdfUrl?, createdBy, createdAt, updatedAt, deletedAt?
```

Lignes : `productId?, ref, title, details[], unit, quantity, unitPriceHT, vatRate, discount, totalHT, order`.

### 10.3 Règles de sécurité (principe)

```
match /companies/{companyId}/{document=**} {
  allow read:  if isMember(companyId);
  allow write: if hasRole(companyId, ['OWNER','MANAGER']);
}
match /companies/{companyId}/clients/{id}     { allow write: if hasRole(companyId, ['OWNER','MANAGER','SALES']); }
match /companies/{companyId}/expenses/{id}    { allow read, write: if hasRole(companyId, ['OWNER','MANAGER']); }
match /companies/{companyId}/members/{uid}    { allow write: if hasRole(companyId, ['OWNER']); }
```
Storage : chemin `companies/{companyId}/…`, accès limité aux membres de la société. Les fichiers `firestore.rules` et `storage.rules` sont livrés avec le projet, avec index composites (`firestore.indexes.json`).

---

## 11. Navigation et disposition des pages

### 11.1 Barre de navigation inférieure (5 entrées)

```
[ Accueil ]  [ Documents ]  [ ( Scanner ) ]  [ Catalogue ]  [ Plus ]
```
Le bouton central **Scanner** est un bouton d'action flottant intégré à la barre.

### 11.2 Graphe de navigation

```
Splash
 └─ Connexion (Google)
     └─ Assistant société / Choix de société
         └─ Principal
             ├─ Accueil (tableau de bord)
             ├─ Documents
             │    ├─ Liste (onglets : Devis | BL | Factures | Commandes)
             │    ├─ Détail document ── Aperçu PDF ── Partage
             │    └─ Édition document ── Sélecteur client ── Sélecteur produit ── Éditeur de ligne
             ├─ Scanner
             │    ├─ Choix du type (Facture | BL fournisseur)
             │    ├─ Capture (ML Kit)
             │    ├─ Traitement IA
             │    └─ Validation (facture → dépense | BL → produits + fournisseur)
             ├─ Catalogue
             │    ├─ Produits (liste, fiche, édition, historique prix, stock)
             │    └─ Services / catégories
             └─ Plus
                  ├─ Clients (liste, fiche, édition)
                  ├─ Fournisseurs (liste, fiche, édition)
                  ├─ Dépenses (liste, détail, édition, catégories)
                  ├─ Paiements & créances
                  ├─ Rapports & exports
                  ├─ Scans (journal)
                  └─ Paramètres (société, numérotation, utilisateurs, sécurité, sauvegarde, compte)
```

### 11.3 Disposition des écrans clés

**Accueil**
```
┌───────────────────────────────┐
│ Bonjour, <prénom>   [société▾]│
│ ┌──────────┐ ┌──────────┐     │
│ │ CA du mois│ │ Impayés  │     │
│ └──────────┘ └──────────┘     │
│ ┌──────────┐ ┌──────────┐     │
│ │ Dépenses │ │ Devis en │     │
│ │ du mois  │ │ attente  │     │
│ └──────────┘ └──────────┘     │
│ [+Devis][+BL][+Facture][Scan] │
│ Graphique recettes / dépenses │
│ Alertes (retards, scans…)     │
│ Derniers documents            │
├───────────────────────────────┤
│ Accueil Docs (Scan) Catal Plus│
└───────────────────────────────┘
```

**Liste des documents** : barre de recherche, onglets (Devis | BL | Factures | Commandes), filtres (statut, période, client), cartes : n° + client + date + montant + pastille de statut ; bouton **+** en bas à droite ; balayage pour dupliquer / supprimer.

**Édition d'un document** : en-tête (type, n° auto, date, client), liste des lignes (désignation, quantité × PU, total), bouton **Ajouter une ligne** (catalogue / libre), remise, TVA, bloc totaux fixe en bas, boutons **Aperçu PDF** et **Valider**.

**Validation d'un scan** : image du document en haut (zoom), formulaire pré-rempli dessous, champs douteux surlignés, lignes avec choix *Lier / Créer / Ignorer* (BL), boutons **Enregistrer** et **Rejeter**.

**Fiche produit** : photo, désignation, référence, prix achat / vente, marge, stock, fournisseurs et historique des prix, documents où le produit apparaît.

---

## 12. Arborescence du code

```
dynic-crm/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json                  (à générer dans Firebase, non versionné)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── dynic_fond_page.png
│       │   │   ├── dynic_entete.png
│       │   │   ├── dynic_pied_de_page.png
│       │   │   └── dynic_cachet.png
│       │   ├── res/                          (thème, icônes, strings.xml en français, polices Carlito)
│       │   └── java/ma/dynic/crm/
│       │       ├── DynicApp.kt               (Application Hilt, init Firebase, App Check)
│       │       ├── MainActivity.kt
│       │       │
│       │       ├── core/
│       │       │   ├── common/               (Result, dispatchers, extensions, constantes)
│       │       │   ├── format/               (MoneyFormatter, DateFormatter, AmountInWords)
│       │       │   ├── validation/           (IceValidator, VatCalculator)
│       │       │   └── permissions/          (RoleChecker)
│       │       │
│       │       ├── domain/
│       │       │   ├── model/                (Company, Member, Client, Supplier, Product, Document,
│       │       │   │                          DocumentLine, Payment, Expense, Scan, StockMovement)
│       │       │   ├── repository/           (interfaces : AuthRepository, ClientRepository, …)
│       │       │   └── usecase/
│       │       │       ├── auth/             (SignInWithGoogle, SignOut, JoinCompany)
│       │       │       ├── document/         (CreateDocument, ConvertDocument, ValidateDocument,
│       │       │       │                      ComputeTotals, NextNumber, GeneratePdf)
│       │       │       ├── payment/          (RecordPayment, ComputeBalance)
│       │       │       ├── scan/             (ScanInvoiceToExpense, ScanDeliveryToProducts,
│       │       │       │                      MatchProducts, DetectDuplicate)
│       │       │       └── report/           (SalesReport, ExpenseReport, ExportCsv)
│       │       │
│       │       ├── data/
│       │       │   ├── remote/
│       │       │   │   ├── firestore/        (FirestorePaths, DTO, mappers)
│       │       │   │   ├── storage/          (FileStorageService)
│       │       │   │   └── ai/               (GeminiExtractionService, prompts, JSON schemas)
│       │       │   ├── repository/           (implémentations : FirestoreClientRepository, …)
│       │       │   └── worker/               (ScanUploadWorker, ReminderWorker)
│       │       │
│       │       ├── pdf/
│       │       │   ├── PdfEngine.kt          (pagination, répétition de l'en-tête de tableau)
│       │       │   ├── PageBackground.kt     (fond + zone interdite du pied de page)
│       │       │   ├── components/           (HeaderBlock, ClientBlock, TitleBlock, LinesTable,
│       │       │   │                          TotalsBlock, StampBlock)
│       │       │   └── templates/            (QuoteTemplate, DeliveryTemplate,
│       │       │                              InvoiceTemplate, PurchaseOrderTemplate)
│       │       │
│       │       ├── scan/
│       │       │   ├── DocumentScannerLauncher.kt   (ML Kit Document Scanner)
│       │       │   └── ImageCompressor.kt
│       │       │
│       │       ├── di/                       (modules Hilt : Firebase, Repository, Pdf, Ai)
│       │       │
│       │       └── ui/
│       │           ├── theme/                (Color #1DB094, Type, Shape, Theme)
│       │           ├── components/           (AppScaffold, BottomBar, StatusChip, MoneyText,
│       │           │                          SearchField, EmptyState, ConfirmDialog, LineEditor)
│       │           ├── navigation/           (AppNavGraph, Routes)
│       │           └── feature/
│       │               ├── splash/
│       │               ├── auth/             (LoginScreen, LoginViewModel)
│       │               ├── onboarding/       (CompanySetupScreen, JoinCompanyScreen)
│       │               ├── dashboard/        (DashboardScreen, DashboardViewModel)
│       │               ├── documents/        (DocumentListScreen, DocumentDetailScreen,
│       │               │                      DocumentEditScreen, PdfPreviewScreen, ViewModels)
│       │               ├── scanner/          (ScanTypeScreen, ScanReviewInvoiceScreen,
│       │               │                      ScanReviewDeliveryScreen, ScanJournalScreen)
│       │               ├── catalog/          (ProductListScreen, ProductDetailScreen, ProductEditScreen)
│       │               ├── clients/
│       │               ├── suppliers/
│       │               ├── expenses/
│       │               ├── payments/
│       │               ├── reports/
│       │               └── settings/         (Company, Numbering, Users, Security, Backup)
│       ├── test/                             (tests unitaires : totaux, numérotation, rapprochement produits)
│       └── androidTest/                      (tests UI Compose, émulateur Firebase)
│
├── firebase/
│   ├── firestore.rules
│   ├── firestore.indexes.json
│   ├── storage.rules
│   └── firebase.json                         (config Emulator Suite)
├── gradle/libs.versions.toml                 (catalogue de versions)
├── build.gradle.kts
├── settings.gradle.kts
└── README.md                                 (installation Firebase, SHA-1, Google Sign-In, App Check)
```

---

## 13. Exigences non fonctionnelles

- **Performance** : liste de 5 000 documents fluide (pagination Firestore), génération d'un PDF d'une page < 2 s.
- **Fiabilité** : fonctionne sans réseau pour toutes les opérations hors scan IA ; aucune perte de données lors des coupures.
- **Sécurité** : règles Firestore/Storage strictes, App Check, biométrie optionnelle, journal d'audit.
- **Ergonomie** : une main, gros boutons, saisie rapide, recherche instantanée, mode sombre.
- **Accessibilité** : tailles de police réglables, contrastes conformes.
- **Coûts maîtrisés** : compression des images, quotas d'IA par société, mise en cache.
- **Maintenabilité** : code modulaire, tests sur les calculs, README de déploiement.

---

## 14. Plan de réalisation

| Phase | Contenu |
|---|---|
| **1 — Socle** | Projet, thème, Firebase, connexion Google, assistant société, rôles, règles de sécurité |
| **2 — Données** | Clients, fournisseurs, produits, paramètres, numérotation |
| **3 — Documents** | Devis, BL, factures, bons de commande, moteur PDF fidèle, partage, conversions |
| **4 — Finances** | Paiements, dépenses, tableau de bord, rapports, exports |
| **5 — Scan IA** | Scan facture → dépense, scan BL → produits + fournisseur, validation, journal |
| **6 — Finitions** | Hors-ligne, notifications, biométrie, tests, publication Play Store (test interne) |

**MVP** = phases 1 à 3 + dépenses manuelles ; le scan IA arrive en phase 5.

---

## 15. Critères d'acceptation

- [ ] Connexion Google fonctionnelle, société créée ou rejointe, rôles appliqués par les règles Firestore.
- [ ] Les 4 types de documents sont générés en PDF **conformes aux modèles joints** (fond, date, client, titre, tableau, totaux, cachet, pied de page).
- [ ] Jeu de test : 11 lignes du devis exemple → Total HT 15 572,00, TVA 3 114,40, TTC 18 686,40 DH.
- [ ] Scan d'une facture fournisseur : dépense créée après validation, doublon détecté.
- [ ] Scan d'un BL fournisseur : fournisseur et produits créés ou liés après validation, stock mis à jour si activé.
- [ ] Utilisation hors-ligne puis synchronisation sans perte ni numéro en double.
- [ ] Aucun contenu dans la zone du pied de page ; pagination correcte.

---

## 16. Points à confirmer (à remplir avant le développement)

1. Règle exacte de numérotation des devis (`TP-27-0100` : signification de « 27 ») et formats des factures et bons de commande.
2. **Bon de commande** : achat fournisseur uniquement, ou aussi commande client à enregistrer ?
3. Stock : activé par défaut, ou simple catalogue de prix ?
4. Mentions légales et conditions de paiement exactes de la facture.
5. Nombre d'utilisateurs prévus et rôles réels.
6. Vente de l'application à d'autres sociétés : si oui, prévoir un **plan d'abonnement** (Google Play Billing) et un mode multi-sociétés complet dès la v1.

---

## 17. Prompt de démarrage conseillé pour Google AI Studio

> « Tu es un développeur Android senior. À partir du cahier des charges joint, génère le projet **dynic-crm** en Kotlin / Jetpack Compose avec l'arborescence de la section 12. Commence par la **phase 1** (connexion Google + Firebase, assistant de création de société, navigation à 5 onglets, thème vert `#1DB094`), puis la **phase 3** pour les PDF : reproduis le PDF de référence joint pixel pour pixel (positions en points de la section 8). Fournis chaque fichier complet, les règles Firestore/Storage et un README d'installation Firebase. »
