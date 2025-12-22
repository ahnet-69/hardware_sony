#
# Copyright (C) 2024 XperiaLabs Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Soong Namespace
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)/XperiaModules

# Audio Settings
ifeq ($(TARGET_SHIPS_SOUND_ENHANCEMENT),true)
PRODUCT_PACKAGES += \
	XperiaAudio
endif

# Battery Care
ifeq ($(TARGET_SUPPORTS_BATT_CARE),true)
include hardware/sony/XperiaModules/XperiaCharger/sepolicy/SEPolicy.mk

PRODUCT_PACKAGES += \
	XperiaCharger
endif

# Devices
ifeq ($(TARGET_SHIPS_SONY_APPS),true)
PRODUCT_PACKAGES += \
	XperiaDevices
endif

# Display Settings
ifeq ($(TARGET_SUPPORTS_CREATOR_MODE),true)
include hardware/sony/XperiaModules/XperiaDisplay/sepolicy/SEPolicy.mk

PRODUCT_PACKAGES += \
	XperiaDisplay
endif

# E-Sim
ifeq ($(TARGET_SUPPORTS_EUICC),true)
PRODUCT_PACKAGES += \
	XperiaEuicc
endif

# High Touch Polling Service
ifeq ($(TARGET_SUPPORTS_HIGH_POLLING_RATE),true)
include hardware/sony/XperiaModules/XperiaTouch/sepolicy/SEPolicy.mk

PRODUCT_PACKAGES += \
	XperiaTouch \
	XperiaTouchOverlay
endif

# Media Vibration
ifeq ($(TARGET_SUPPORTS_MEDIA_VIBRATION),true)
PRODUCT_PACKAGES += \
	XperiaHaptics
endif

# Refresh Rate
ifeq ($(TARGET_SUPPORTS_HIGH_REFRESH_RATE),true)
PRODUCT_PACKAGES += \
	XperiaSwitcher
endif

# Apps Disabler
ifeq ($(TARGET_SHIPS_XPERIA_DISABLER),true)
include hardware/sony/XperiaModules/XperiaDisabler/sepolicy/SEPolicy.mk

PRODUCT_PACKAGES += \
	XperiaDisabler
endif
