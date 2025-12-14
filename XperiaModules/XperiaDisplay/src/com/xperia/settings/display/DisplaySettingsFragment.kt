/*
 * Copyright (C) 2025 XperiaLabs Project
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xperia.settings.display

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.SystemProperties
import android.provider.Settings
import android.util.ArraySet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.hardware.display.ColorDisplayManager

import androidx.preference.*
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

import com.android.internal.util.ArrayUtils
import com.android.settingslib.widget.LayoutPreference

import java.util.*
import kotlin.collections.ArrayList

import com.xperia.settings.display.R
import com.xperia.settings.display.SemcDisplayUtils.Companion.CREATOR_MODE_ENABLE
import com.xperia.settings.display.SemcDisplayUtils.Companion.MOTION_BLUR_REDUCTION_ENABLE
import com.xperia.settings.display.SemcDisplayUtils.Companion.WHITE_BALANCE_PROF

import com.android.settingslib.widget.SettingsBasePreferenceFragment

class DisplaySettingsFragment : SettingsBasePreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val DOT_INDICATOR_SIZE = 12
        private const val DOT_INDICATOR_LEFT_PADDING = 6
        private const val DOT_INDICATOR_RIGHT_PADDING = 6

        const val CREATOR_MODE_KEY = "switchCreatorMode"
        const val WHITE_BALANCE_KEY = "white_balance_switch"
        const val MOTION_BLUR_KEY = "motion_blur_reduction"

        const val DEVICE_GET_PROP = "ro.yaap.device"
    }

    private lateinit var semcDisplayUtils: SemcDisplayUtils

    private val KEY_CREATOR_MODE_PREVIEW = "creator_mode_preview"
    private val PAGE_VIEWER_SELECTION_INDEX = "page_viewer_selection_index"

    private var mViewArrowPrevious: View? = null
    private var mViewArrowNext: View? = null
    private var mViewPager: ViewPager? = null

    private var mPageList: ArrayList<View>? = null

    private var mDotIndicators: Array<ImageView?>? = null
    private var mViewPagerImages: Array<View?>? = null

    private lateinit var creatorModePreference: SwitchPreferenceCompat
    private lateinit var motionBlurPreference: SwitchPreferenceCompat
    private lateinit var whiteBalancePreference: ListPreference

    private val settingsObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            creatorModePreference.isChecked = semcDisplayUtils.isCMEnabled()

            motionBlurPreference.isChecked = semcDisplayUtils.isMotionBlurReductionEnabled()

            whiteBalancePreference.isEnabled = !semcDisplayUtils.isCMEnabled()
            whiteBalancePreference.setValueIndex(semcDisplayUtils.getWbProfile())
        }
    }

    private fun getDevice(): String {
        return SystemProperties.get(DEVICE_GET_PROP, "")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.display_settings)
        semcDisplayUtils = SemcDisplayUtils(requireContext())

        addViewPager()

        creatorModePreference = findPreference<SwitchPreferenceCompat>(CREATOR_MODE_KEY)!!
        creatorModePreference.isChecked = semcDisplayUtils.isCMEnabled()
        creatorModePreference.onPreferenceChangeListener = this

        whiteBalancePreference = findPreference<ListPreference>(WHITE_BALANCE_KEY)!!
        whiteBalancePreference.isEnabled = !semcDisplayUtils.isCMEnabled()
        whiteBalancePreference.setValueIndex(semcDisplayUtils.getWbProfile())
        whiteBalancePreference.onPreferenceChangeListener = this

        motionBlurPreference = findPreference<SwitchPreferenceCompat>(MOTION_BLUR_KEY)!!
        motionBlurPreference.isChecked = semcDisplayUtils.isMotionBlurReductionEnabled()
        motionBlurPreference.onPreferenceChangeListener = this

        // Hide toggle on other devices as only pdx203 supports this feature (60Hz display)
        if (getDevice() != "pdx203") {
            val displayCategory = findPreference<PreferenceCategory>("display_cm_header_category")!!
            displayCategory.removePreference(motionBlurPreference)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            MOTION_BLUR_KEY -> {
                semcDisplayUtils.setMotionBlurReductionEnabled(newValue as Boolean)
            }
            WHITE_BALANCE_KEY -> {
                semcDisplayUtils.setWhiteBalance((newValue as String).toInt())
            }
            CREATOR_MODE_KEY -> {
                semcDisplayUtils.setCMMode(newValue as Boolean)
            }
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PAGE_VIEWER_SELECTION_INDEX, mViewPager!!.currentItem)
    }

    fun getViewPagerResource(): ArrayList<Int> {
        return arrayListOf(
            R.layout.creator_mode_view1,
            R.layout.creator_mode_view2,
            R.layout.creator_mode_view3)
    }

    fun addViewPager() {
        val preview = findPreference<LayoutPreference>(KEY_CREATOR_MODE_PREVIEW)
        val tmpviewPagerList = getViewPagerResource()
        mViewPager = preview!!.findViewById(R.id.viewpager)

        mViewPagerImages = arrayOfNulls<View>(3)
        for (idx in tmpviewPagerList.indices) {
            mViewPagerImages!![idx] = layoutInflater.inflate(tmpviewPagerList[idx], null /* root */)
        }

        mPageList = ArrayList()
        mPageList!!.add(mViewPagerImages!![0]!!)
        mPageList!!.add(mViewPagerImages!![1]!!)
        mPageList!!.add(mViewPagerImages!![2]!!)

        mViewPager!!.adapter = ColorPagerAdapter(mPageList!!)

        mViewArrowPrevious = preview.findViewById(R.id.arrow_previous)
        mViewArrowPrevious?.setOnClickListener { v ->
            val previousPos = mViewPager!!.currentItem - 1
            mViewPager!!.setCurrentItem(previousPos, true)
        }

        mViewArrowNext = preview.findViewById(R.id.arrow_next)
        mViewArrowNext?.setOnClickListener { v ->
            val nextPos = mViewPager!!.currentItem + 1
            mViewPager!!.setCurrentItem(nextPos, true)
        }

        mViewPager!!.addOnPageChangeListener(createPageListener())

        val viewGroup = preview.findViewById<ViewGroup>(R.id.viewGroup)
        mDotIndicators = arrayOfNulls(mPageList!!.size)
        for (i in mPageList!!.indices) {
            val imageView = ImageView(requireContext())
            val lp = ViewGroup.MarginLayoutParams(DOT_INDICATOR_SIZE, DOT_INDICATOR_SIZE)
            lp.setMargins(DOT_INDICATOR_LEFT_PADDING, 0, DOT_INDICATOR_RIGHT_PADDING, 0)
            imageView.layoutParams = lp
            mDotIndicators!![i] = imageView

            viewGroup.addView(mDotIndicators!![i])
        }

        updateIndicator(mViewPager!!.currentItem)
    }

    fun createPageListener(): ViewPager.OnPageChangeListener {
        return object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset != 0f) {
                    for (idx in mPageList!!.indices) {
                        mViewPagerImages!![idx]!!.setVisibility(View.VISIBLE)
                    }
                } else {
                    mViewPagerImages!![position]!!.contentDescription =
                    requireContext().getString(R.string.creator_mode_content_description)
                    updateIndicator(position)
                }
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        }
    }

    private fun updateIndicator(position: Int) {
        for (i in mPageList!!.indices) {
            if (position == i) {
                mDotIndicators!![i]!!.setBackgroundResource(R.drawable.ic_creator_mode_indicator_focused)
                mViewPagerImages!![i]!!.visibility = View.VISIBLE
            } else {
                mDotIndicators!![i]!!.setBackgroundResource(R.drawable.ic_creator_mode_indicator_unfocused)
                mViewPagerImages!![i]!!.visibility = View.INVISIBLE
            }
        }

        if (position == 0) {
            mViewArrowPrevious!!.visibility = View.INVISIBLE
            mViewArrowNext!!.visibility = View.VISIBLE
        } else if (position == (mPageList!!.size - 1)) {
            mViewArrowPrevious!!.visibility = View.VISIBLE
            mViewArrowNext!!.visibility = View.INVISIBLE
        } else {
            mViewArrowPrevious!!.visibility = View.VISIBLE
            mViewArrowNext!!.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().contentResolver?.registerContentObserver(
            Settings.Global.getUriFor(CREATOR_MODE_ENABLE),
            true,
            settingsObserver
        )
        requireContext().contentResolver?.registerContentObserver(
            Settings.Global.getUriFor(MOTION_BLUR_REDUCTION_ENABLE),
            true,
            settingsObserver
        )
        requireContext().contentResolver?.registerContentObserver(
            Settings.Global.getUriFor(WHITE_BALANCE_PROF),
            true,
            settingsObserver
        )

        settingsObserver.onChange(false, null)
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(settingsObserver)
    }

    class ColorPagerAdapter(private val mPageViewList: ArrayList<View>) : PagerAdapter() {
        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            if (mPageViewList[position] != null) {
                container.removeView(mPageViewList[position])
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            container.addView(mPageViewList[position])
            return mPageViewList[position]
        }

        override fun getCount(): Int {
            return mPageViewList.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return `object` == view
        }
    }
}
