# Griniaris JavaFX Multiplayer
Δικτυακό επιτραπέζιο «Γκρινιάρης» με αρχιτεκτονική client–server (JavaFX).

## Run (ενδεικτικά)
- Server: `./gradlew :server:run` ή `java -jar server.jar`
- Client: `./gradlew :client:run` ή `java -jar client.jar`

## Δομή (πρόταση)
gr.uop.griniaris
 ├─ client/   # JavaFX UI
 ├─ server/   # game engine + sockets
 └─ shared/   # models & messages

## Credits
Based on university team project. Rewritten/organized in προσωπικό repo.
