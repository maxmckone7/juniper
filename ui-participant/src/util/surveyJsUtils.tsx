import classNames from 'classnames'
import { get, set } from 'lodash'
import _union from 'lodash/union'
import _keys from 'lodash/keys'
import _isEqual from 'lodash/isEqual'
import { micromark } from 'micromark'
import React, { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SurveyModel } from 'survey-core'
import { Survey as SurveyJSComponent } from 'survey-react-ui'

import { SURVEY_JS_OTHER_SUFFIX, surveyJSModelFromForm, SurveyJsResumeData } from '@juniper/ui-core'

import { Answer, ConsentForm, Profile, Survey, UserResumeData } from 'api/api'
import { usePortalEnv } from 'providers/PortalProvider'

const PAGE_NUMBER_PARAM_NAME = 'page'

/** used for paging surveys */
export type PageNumberControl = {
  pageNumber: number | null,
  updatePageNumber: (page: number) => void
}

/**
 * hook for reading/writing pageNumbers to url search params as 'page'
 * */
export function useRoutablePageNumber(): PageNumberControl {
  const [searchParams, setSearchParams] = useSearchParams()
  const pageParam = searchParams.get(PAGE_NUMBER_PARAM_NAME)
  let urlPageNumber = null
  if (pageParam) {
    urlPageNumber = parseInt(pageParam)
  }

  /** update the url with the new page number */
  function updatePageNumber(newPageNumber: number) {
    searchParams.set('page', (newPageNumber).toString())
    setSearchParams(searchParams)
  }

  return {
    pageNumber: urlPageNumber,
    updatePageNumber
  }
}

type UseSurveyJsModelOpts = {
  extraCssClasses?: Record<string, string>
}

/**
 * handle setting up the surveyJS model for the given form/survey.
 * Two main goals are:
 *  1. provide a single interface point between our components and surveyJs so we can augment as needed
 *  2. Keep the SurveyJS model that we create in state, so that a rerender of this component does not destroy
 *  survey progress
 *
 * @param form a survey or ConsentForm - expects a content property that is a string that specifies the surveyJS survey
 * @param resumeData surveyJS resumable data, taken from surveyJSModel.data.  Note that the 'currentPageNo' of this
 * object will be ignored in favor of the pageNumber param below
 * @param onComplete handler for when the survey is complete.  Note that surveyjs by default will immediately hide the
 * survey on completion and display a completion banner.  To continue displaying the form, use the
 * `refreshSurvey` function
 * @param pager the control object for paging the survey
 * @param profile
 * @param opts optional configuration for the survey
 * @param opts.extraCssClasses mapping of element to CSS classes to add to that element. See
 * https://surveyjs.io/form-library/examples/survey-customcss/reactjs#content-docs for a list of available elements.
 */
export function useSurveyJSModel(
  form: ConsentForm | Survey,
  resumeData: SurveyJsResumeData | null,
  onComplete: () => void,
  pager: PageNumberControl,
  profile?: Profile,
  opts: UseSurveyJsModelOpts = {}
) {
  const {
    extraCssClasses = {}
  } = opts

  const { portalEnv } = usePortalEnv()

  const [surveyModel, setSurveyModel] = useState<SurveyModel>(newSurveyJSModel(resumeData, pager.pageNumber))

  /** hand a page change by updating state of both the surveyJS model and our internal state*/
  function handlePageChanged(model: SurveyModel, options: any) { // eslint-disable-line @typescript-eslint/no-explicit-any, max-len
    const newPage = options.newCurrentPage.num
    pager.updatePageNumber(newPage)
  }

  /** returns a surveyJS survey model with the given data/pageNumber */
  function newSurveyJSModel(refreshData: SurveyJsResumeData | null, pagerPageNumber: number | null) {
    const newSurveyModel = surveyJSModelFromForm(form)

    Object.entries(extraCssClasses).forEach(([elementPath, className]) => {
      set(newSurveyModel.css, elementPath, classNames(get(newSurveyModel.css, elementPath), className))
    })

    if (refreshData) {
      newSurveyModel.data = refreshData.data
    }

    // default to first page
    let pageNumber = 0
    if (pagerPageNumber != null) {
      // if pager page is specified, use that
      // the user-visible page param is 1-indexed, but surveyJS page numbers are 0-indexed
      pageNumber = pagerPageNumber - 1
    } else if (refreshData) {
      // otherwise pick up where the user left off
      pageNumber = refreshData.currentPageNo
    }
    newSurveyModel.currentPageNo = pageNumber
    newSurveyModel.setVariable('profile', profile)
    newSurveyModel.setVariable('portalEnvironmentName', portalEnv.environmentName)
    newSurveyModel.onComplete.add(onComplete)
    newSurveyModel.onCurrentPageChanged.add(handlePageChanged)
    newSurveyModel.onTextMarkdown.add(applyMarkdown)
    newSurveyModel.completedHtml = '<div></div>'  // the application UX will handle showing any needed messages
    return newSurveyModel
  }

  const refreshSurvey = (refreshData: SurveyJsResumeData | null, pagerPageNumber: number | null) => {
    setSurveyModel(newSurveyJSModel(refreshData, pagerPageNumber))
  }

  // handle external page number changes, e.g. browser back button
  useEffect(() => {
    if (surveyModel && pager.pageNumber != null) {
      surveyModel.currentPageNo = pager.pageNumber - 1
    }
  }, [pager.pageNumber])

  const pageNumber = surveyModel ? surveyModel.currentPageNo + 1 : 1
  const SurveyComponent = surveyModel ? <SurveyJSComponent model={surveyModel}/> : <></>
  return { surveyModel, refreshSurvey, pageNumber, SurveyComponent, setSurveyModel }
}

// TODO: Add JSDoc
// eslint-disable-next-line jsdoc/require-jsdoc
export const applyMarkdown = (survey: object, options: { text: string, html: string }) => {
  const markdownText = micromark(options.text)
  // chop off <p> tags.
  // See https://surveyjs.io/form-library/examples/edit-survey-questions-markdown/reactjs#content-code
  if (markdownText.startsWith('<p>') && markdownText.endsWith('</p>')) {
    options.html = markdownText.substring(3, markdownText.length - 4)
  }
}

export type SurveyJsItem = {
  name: string | number,
  title: string,
  value: object,
  displayValue: string
}

export type SurveyJsValueType = string | boolean | number | object | null

/**
 * get resumeData suitable for including on a form response, current a map of userId -> data
 * resetPageNumber can be set to true for cases where the user will expect to go back to the beginning
 * of the survey next time they visit it (such as after completing a survey)
 * */
export function getResumeData(surveyJSModel: SurveyModel,
  participantUserId: string | null,
  resetPageNumber = false): string {
  const resumeData: Record<string, UserResumeData> = {}
  if (participantUserId) {
    // if this is a complete submission, the user will expect to come back to the beginning
    const currentPageNo = resetPageNumber ? 1 : surveyJSModel.currentPageNo + 1
    resumeData[participantUserId] = { currentPageNo }
  }
  return JSON.stringify(resumeData)
}

/** converts the given model into a list of answers, or an empty array if undefined */
export function getSurveyJsAnswerList(surveyJSModel: SurveyModel): Answer[] {
  if (!surveyJSModel.data) {
    return []
  }
  return Object.entries(surveyJSModel.data)
    // don't make answers for the descriptive sections
    .filter(([key]) => {
      return !key.endsWith(SURVEY_JS_OTHER_SUFFIX) && surveyJSModel.getQuestionByName(key)?.getType() !== 'html'
    })
    .map(([key, value]) => makeAnswer(value as SurveyJsValueType, key, surveyJSModel.data))
}

/** return an Answer for the given value.  This should be updated to take some sort of questionType/dataType param */
export function makeAnswer(value: SurveyJsValueType, questionStableId: string,
  surveyJsData: Record<string, SurveyJsValueType>): Answer {
  const answer: Answer = { questionStableId }
  if (typeof value === 'string') {
    answer.stringValue = value
  } else if (typeof value == 'number') {
    answer.numberValue = value
  } else if (typeof value == 'boolean') {
    answer.booleanValue = value
  } else if (value) {
    answer.objectValue = JSON.stringify(value)
  }
  if (surveyJsData[questionStableId + SURVEY_JS_OTHER_SUFFIX]) {
    // surveyJS "other" descriptions are always strings
    answer.otherDescription = surveyJsData[questionStableId + SURVEY_JS_OTHER_SUFFIX] as string
  } else if (questionStableId.endsWith(SURVEY_JS_OTHER_SUFFIX)) {
    const baseStableId = questionStableId.substring(0, questionStableId.lastIndexOf('-'))
    return makeAnswer(surveyJsData[baseStableId], baseStableId, surveyJsData)
  }
  return answer
}

/** compares two surveyModel.data objects and returns a list of answers corresponding to updates */
export function getUpdatedAnswers(original: Record<string, SurveyJsValueType>,
  updated: Record<string, SurveyJsValueType>): Answer[] {
  const allKeys = _union(_keys(original), _keys(updated))
  const updatedKeys = allKeys.filter(key => !_isEqual(original[key], updated[key]))
    .map(key => key.endsWith(SURVEY_JS_OTHER_SUFFIX) ? key.substring(0, key.lastIndexOf(SURVEY_JS_OTHER_SUFFIX)) : key)
  const dedupedKeys = Array.from(new Set(updatedKeys).values())

  return dedupedKeys.map(key => makeAnswer(updated[key], key, updated))
}

/** get a merge of both the explicit answer data and the calculated values */
export function getDataWithCalculatedValues(model: SurveyModel) {
  const calculatedHash: Record<string, object> = {}
  model.calculatedValues.forEach(val => {
    if (val.includeIntoResult) {
      calculatedHash[val.name] = val.value
    }
  })
  return {
    ...model.data,
    ...calculatedHash
  }
}
