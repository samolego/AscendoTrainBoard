# AscendoTrainBoard

AscendoTrainBoard je self-hosted aplikacija za beleženje balvanskih plezalnih smeri, ki jo uporabljamo v naši plezalni sekciji Ascendo. 

Uporabniki lahko dodajajo lastne plezalne probleme (balvane), jih pregledajo na slikah sektorjev z označenimi oprimki ter jim dodeljujejo težavnosti in ocene. Aplikacija spodbuja soustvarjanje in skupnostno ocenjevanje, obenem pa omogoča preprosto upravljanje in deljenje plezalnih smeri.

## Posnetki zaslona

| <img width="256" src="metadata/en-US/images/phoneScreenshots/screenshot_list_boulders.png"> | <img width="256" src="metadata/en-US/images/phoneScreenshots/screenshot_view.png"> | <img width="256" src="metadata/en-US/images/phoneScreenshots/screenshot_create.png"> |
|:---:|:---:|:---:|
| Preglej smeri | Prikaži opriimke | Ustvarjaj smeri |

## Glavne funkcionalnosti

* Prijava in registracija: Osnovni uporabniški računi, podprte seje.
* Urejanje smeri: Dodajanje, spreminjanje in brisanje balvanov z zaporedji oprimkov na vnaprej pripravljenih sektorjih.
* Sistem ocenjevanja: Uporabniki lahko predlagajo težavnosti in ocenjujejo probleme (1–5 zvezdic); povprečja se izračunajo samodejno.
* Upravljanje sektorjev: Statični sektorji z definiranimi oprimki in slikami, ki jih sistem zazna iz datotečnega sistema.
* Spletna in mobilna aplikacija: narejena v Kotlin Multiplatform za enotno uporabniško izkušnjo.
* Preprost backend: Strežnik v Rustu, optimiziran za majhne naprave (ESP32, Raspberry Pi).
* Self-hosted: Popoln nadzor nad podatki in zasebnostjo.
* REST API: Celovito dokumentiran v OpenAPI specifikaciji.
* Administracija: Skrbniki lahko upravljajo vse probleme in uporabnike.

## Arhitektura

AscendoTrainBoard sestavljata dva glavna dela:

### Backend

Zaledni del je napisan v jeziku Rust. Aplikaciji izpostavlja REST API, upravlja z uporabniški računi, skrbi za shranjevanje podatkov ipd. Je preprost & *lightweight*, tako da deluje tudi na šibkejših napravah (beri: raspberry pi).

Podatki se zaradi preprostosti shranjujejo v JSON datotekah.
Vsebujejo informacije o uporabnikih, smereh in nastavitvah, sistem pa jih periodično samodejno shranjuje. Varnost je zagotovljena s hashanjem gesel s SHA256 in soljenjem.

**Za varno komunikacijo je treba dodati še HTTPS certifikat.**

### Aplikacija (Frontend)

Odjemalska aplikacija je napisana v Kotlin Multiplatform in je namenjena uporabu v spletnem brskalniku in na Androidu. Uporabnikom nudi vmesnik za pregledovanje sektorjev, ustvarjanje plezalnih smeri ter oddajanje ocen in težavnosti. Z backendom komunicira preko REST APIja.

## Razvoj aplikacije

Zahtevana orodja
* Rust razvojno okolje
* Kotlin Multiplatform (priporočen Android Studio)
* Node.js in npm (za OpenAPI generator)
