# Using GitHub Actions for browser build

These instructions allow you to build AndroidAPS with a browser.


## Setup Github AndroidAPS repository

1. Fork https://github.com/nightscout/AndroidAPS into your account. If you already have a fork of AndroidAPS in GitHub, you can't make another one. You can continue to work with your existing fork, or delete that from GitHub and then fork https://github.com/nightscout/AndroidAPS.


## Prepare your existing or new Android Studio signing keystore

1. Base64 encoding of the keystore file (on a Windows PC):\
   certutil -encode keystore.jks SIGNING_KEY.txt
2. Base64 encoding of the keystore file (on other plattforms with openssl):\
   openssl base64 < keystore.jks | tr -d '\n' | tee SIGNING_KEY.txt
3. Base64 encoding of the keystore file (on macOS):\
   base64 keystore.jks > SIGNING_KEY.txt


## Setup Github secrets for the keystore

1. In the forked AndroidAPS repository, go to Settings -> Secrets and variables -> Actions.
1. For each of the following secrets, tap on "New repository secret", then add the name of the secret, along with the value you defined during keystore creation time. As value for the secret SIGNING_KEY use the text out of the file SIGNING_KEY.txt, which is stored between the two lines --BEGIN CERTIFICATE-- and --END CERTIFICATE--.  
    * `KEY_STORE_PASSWORD`
    * `KEY_ALIAS`
    * `KEY_PASSWORD`
    * `SIGNING_KEY`


## Build AndroidAPS
1. On your forked AndroidAPS repository, go to Actions.
2. If not already done, activate workflow on your repository.
3. On the left side, select the workflow "Build app version".
4. On the right side, click on the drop down menue "Run workflow" and select "Branch: master" which is the default value.
5. Then click on "Run workflow".


## Download the build and signed app
1. When the workflow (build and sign) is completed, click on "Build app version" at the right side.\
   There should be a green checkmark in front of this text when the workflow was sucessfull.
2. Scroll down to the block "Annotations".
3. There, click on the download link behind the ZIP file, which contains the signed app.
4. After the download completes, delete the ZIP file on Github:\
   click at the trash behind the ZIP file.\
   This saves space at your Github account and protects your data.
6. Uncompress the ZIP file. It contains the file, named app-full-release-unsigned-signed.apk
7. Rename your app file if necessary.
