# Nov 23

- When running the client and server in two separate locations, files are stored at the server location.
- Use text/serialized files as a simple database for storing configurations and encrypted user files.
- Setup user registration for handling file permissions.
    - File on server (configurations/users) containing a serialized `HashMap<String, String>` of usernames and their public keys.
