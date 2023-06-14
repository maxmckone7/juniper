import React, { useState } from 'react'
import { Tab, Tabs } from 'react-bootstrap'

import { FormContent } from '@juniper/ui-core'

import { FormDesigner } from './FormDesigner'
import { OnChangeFormContent } from './formEditorTypes'
import { FormContentJsonEditor } from './FormContentJsonEditor'
import { FormPreview } from './FormPreview'

type FormContentEditorProps = {
  initialContent: string
  readOnly: boolean
  onChange: OnChangeFormContent
}

// TODO: Add JSDoc
// eslint-disable-next-line jsdoc/require-jsdoc
export const FormContentEditor = (props: FormContentEditorProps) => {
  const { initialContent, readOnly, onChange } = props

  const [activeTab, setActiveTab] = useState<string | null>('designer')
  const [tabsEnabled, setTabsEnabled] = useState(true)

  const [editedContent, setEditedContent] = useState(() => JSON.parse(initialContent) as FormContent)

  return (
    <div className="FormContentEditor d-flex flex-column flex-grow-1">
      <Tabs
        activeKey={activeTab ?? undefined}
        className="mb-1"
        mountOnEnter
        unmountOnExit
        onSelect={setActiveTab}
      >
        <Tab
          disabled={activeTab !== 'designer' && !tabsEnabled}
          eventKey="designer"
          title="Designer"
        >
          <FormDesigner
            value={editedContent}
            onChange={setEditedContent}
          />
        </Tab>
        <Tab
          disabled={activeTab !== 'json' && !tabsEnabled}
          eventKey="json"
          title="JSON Editor"
        >
          <FormContentJsonEditor
            initialValue={editedContent}
            readOnly={readOnly}
            onChange={(isValid, newContent) => {
              if (isValid) {
                setEditedContent(newContent)
                onChange(true, newContent)
              } else {
                onChange(false, undefined)
              }
              setTabsEnabled(isValid)
            }}
          />
        </Tab>
        <Tab
          disabled={activeTab !== 'preview' && !tabsEnabled}
          eventKey="preview"
          title="Preview"
        >
          <FormPreview formContent={editedContent} />
        </Tab>
      </Tabs>
    </div>
  )
}
