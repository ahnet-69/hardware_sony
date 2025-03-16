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

# Flags
TARGET_SUPPORTS_CREATOR_MODE ?= false
TARGET_SUPPORTS_MEDIA_VIBRATION ?= false
TARGET_SUPPORTS_HIGH_REFRESH_RATE ?= false
TARGET_SUPPORTS_HIGH_POLLING_RATE ?= false
TARGET_SUPPORTS_SOUND_ENHANCEMENT ?= false
TARGET_SUPPORTS_SOUND_ENHANCEMENT_DTS ?= false
TARGET_SUPPORTS_BATTERY_CARE ?= false
TARGET_SUPPORTS_EUICC ?= false

# Soong Namespace
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)/XperiaModules

# Devices
ifeq ($(TARGET_SHIPS_SONY_APPS),true)
        PRODUCT_PACKAGES += XperiaDevices
endif

# Battery Care
ifeq ($(TARGET_SUPPORTS_BATTERY_CARE),true)
        BOARD_VENDOR_SEPOLICY_DIRS += hardware/sony/XperiaModules/XperiaCharger/sepolicy/vendor

	PRODUCT_PACKAGES += XperiaCharger
endif

# Display Settings
ifeq ($(TARGET_SUPPORTS_CREATOR_MODE),true)
	PRODUCT_PACKAGES += XperiaDisplay
endif

# Media Vibration
ifeq ($(TARGET_SUPPORTS_MEDIA_VIBRATION),true)
        PRODUCT_PACKAGES += XperiaHaptics
endif

# Refresh Rate
ifeq ($(TARGET_SUPPORTS_HIGH_REFRESH_RATE),true)
	PRODUCT_PACKAGES += XperiaSwitcher
endif

# High Touch Polling Service
ifeq ($(TARGET_SUPPORTS_HIGH_POLLING_RATE),true)
include hardware/sony/XperiaModules/XperiaTouch/sepolicy/SEPolicy.mk
	PRODUCT_PACKAGES += \
	XperiaTouch \
	XperiaTouchOverlay
endif

# Audio Settings
ifeq ($(TARGET_SUPPORTS_SOUND_ENHANCEMENT),true)
       PRODUCT_PACKAGES += XperiaAudio
endif

# E-Sim
ifeq ($(TARGET_SUPPORTS_EUICC),true)
	PRODUCT_PACKAGES += XperiaEuicc
endif
