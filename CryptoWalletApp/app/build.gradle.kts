/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.gms.google-services'
}

android {
    namespace 'com.cryptowallet'
    compileSdk rootProject.ext.compileSdk
    ndkVersion rootProject.ext.ndk
    buildToolsVersion rootProject.ext.buildTools

    defaultConfig {
        applicationId "com.cryptowallet"
        minSdk rootProject.ext.minSdk
        targetSdk rootProject.ext.compileSdk
        versionCode 2
        versionName '1.6'
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility rootProject.ext.jvmTarget
        targetCompatibility rootProject.ext.jvmTarget
    }

    kotlinOptions {
        jvmTarget = rootProject.ext.jvmTarget.toString()
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation project(path: ':core')

    implementation "org.jetbrains.kotlin:kotlin-stdlib:${rootProject.ext.kotlin}"

    // Android X libraries
    implementation 'androidx.core:core-ktx:1.9.0'
    implementation 'androidx.appcompat:appcompat:1.5.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    implementation 'androidx.preference:preference:1.2.0'
    implementation 'androidx.recyclerview:recyclerview:1.2.1'
    implementation 'androidx.gridlayout:gridlayout:1.0.0'
    implementation 'androidx.lifecycle:lifecycle-extensions:2.2.0'
    implementation 'androidx.navigation:navigation-fragment:2.5.3'
    implementation 'androidx.navigation:navigation-ui:2.5.3'
    implementation 'androidx.biometric:biometric:1.1.0'

    implementation 'com.android.volley:volley:1.2.1'

    // Google libraries
    implementation 'com.google.firebase:firebase-messaging:23.1.0'
    implementation 'com.google.android.material:material:1.7.0'
    implementation 'com.google.guava:guava:31.1-jre'
    implementation 'com.google.zxing:core:3.5.1'

    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

    // Networking libraries
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

    // Bitcoin support libraries
    implementation 'org.bitcoinj:bitcoinj-core:0.16.1'
    implementation 'org.conscrypt:conscrypt-android:2.5.2'
    implementation 'org.slf4j:slf4j-android:1.7.36'
    implementation project(path: ':bitcoin')

    // Unit testing libraries
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.9.0'

    // Android instrumentation testing libraries
    androidTestImplementation 'org.mockito:mockito-android:4.9.0'
    androidTestImplementation 'androidx.test:runner:1.5.1'
    androidTestImplementation 'androidx.test.ext:junit:1.1.4'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.0'
}
