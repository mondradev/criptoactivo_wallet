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
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.cryptowallet'
    compileSdk rootProject.ext.compileSdk
    ndkVersion rootProject.ext.ndk
    buildToolsVersion rootProject.ext.buildTools

    defaultConfig {
        minSdk rootProject.ext.minSdk
        targetSdk rootProject.ext.compileSdk

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles "consumer-rules.pro"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
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

    implementation "org.jetbrains.kotlin:kotlin-stdlib:${rootProject.ext.kotlin}"

    // Android X libraries
    implementation 'androidx.core:core-ktx:1.9.0'
    implementation 'androidx.appcompat:appcompat:1.5.1'

    // Google libraries
    implementation 'com.google.android.material:material:1.7.0'

    // Unit testing libraries
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.9.0'

    // Android instrumentation testing libraries
    androidTestImplementation 'org.mockito:mockito-android:4.9.0'
    androidTestImplementation 'androidx.test:runner:1.5.1'
    androidTestImplementation 'androidx.test.ext:junit:1.1.4'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.0'
}