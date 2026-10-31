import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type EmailTemplateCode =
  | 'ACCOUNT_VERIFICATION'
  | 'PASSWORD_RESET'
  | 'MEDICATION_REMINDER_OFFLINE'

export interface EmailTemplate {
  code: EmailTemplateCode
  displayName: string
  subjectTemplate: string
  htmlBodyTemplate: string
  textBodyTemplate: string
  allowedVariables: string[]
  version: number
  updatedAt: string
  updatedByType: string | null
  updatedById: number | null
}

export interface EmailTemplateContent {
  subjectTemplate: string
  htmlBodyTemplate: string
  textBodyTemplate: string
}

export interface RenderedEmail {
  subject: string
  htmlBody: string
  textBody: string
}

export async function listEmailTemplates(): Promise<EmailTemplate[]> {
  try {
    const { data } = await cmsApiClient.get<EmailTemplate[]>('/cms/email-templates')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách mẫu email.')
  }
}

export async function updateEmailTemplate(
  code: EmailTemplateCode,
  content: EmailTemplateContent,
  version: number,
): Promise<EmailTemplate> {
  try {
    const { data } = await cmsApiClient.put<EmailTemplate>(`/cms/email-templates/${code}`, {
      ...content,
      version,
    })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể lưu mẫu email.')
  }
}

export async function previewEmailTemplate(
  code: EmailTemplateCode,
  content: EmailTemplateContent,
): Promise<RenderedEmail> {
  try {
    const { data } = await cmsApiClient.post<RenderedEmail>('/cms/email-templates/preview', {
      code,
      ...content,
    })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tạo bản xem trước.')
  }
}

export async function sendTestEmail(code: EmailTemplateCode, recipientEmail: string): Promise<string> {
  try {
    const { data } = await cmsApiClient.post<{ message: string }>(`/cms/email-templates/${code}/test`, {
      recipientEmail,
    })
    return data.message
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể đưa email thử vào hàng đợi.')
  }
}
