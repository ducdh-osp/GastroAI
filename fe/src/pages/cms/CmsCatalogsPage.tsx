import {
  AppstoreOutlined,
  BookOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  FilterOutlined,
  InfoCircleOutlined,
  MedicineBoxOutlined,
  PlusCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Badge,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  notification,
  Popconfirm,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createCmsCatalog,
  deleteCmsCatalog,
  listCmsCatalogs,
  toggleCmsCatalogActive,
  updateCmsCatalog,
  type CatalogType,
  type CmsCatalogItem,
  type CmsCatalogItemRequest,
} from '../../api/cmsCatalogs'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'

const { Title, Text } = Typography

// Danh sách mã Triage cấp cứu y khoa tiêu hóa để khớp từ khóa tầng 1 Triage
const TRIAGE_OPTIONS = [
  { value: 'BLACK_OR_BLOODY_STOOL', label: 'BLACK_OR_BLOODY_STOOL - Phân đen / Xuất huyết tiêu hóa dưới' },
  { value: 'BLOOD_IN_VOMIT', label: 'BLOOD_IN_VOMIT - Nôn ra máu / chất bã cà phê' },
  { value: 'ABDOMINAL_PAIN', label: 'ABDOMINAL_PAIN - Đau bụng cấp tính dữ dội' },
  { value: 'BLOATING', label: 'BLOATING - Đầy hơi chướng bụng căng cứng' },
  { value: 'CONSTIPATION', label: 'CONSTIPATION - Táo bón kéo dài / bí đại tiện' },
  { value: 'DIARRHEA', label: 'DIARRHEA - Tiêu chảy cấp tính mất nước' },
  { value: 'HEARTBURN', label: 'HEARTBURN - Ợ nóng, trào ngược nghiêm trọng' },
  { value: 'NAUSEA', label: 'NAUSEA - Buồn nôn kéo dài' },
  { value: 'VOMITING', label: 'VOMITING - Nôn mửa liên tục' },
  { value: 'XUAT_HUYET_TIEU_HOA', label: 'XUAT_HUYET_TIEU_HOA - Xuất huyết đường tiêu hóa' },
  { value: 'DAU_BUNG_CAP_TINH', label: 'DAU_BUNG_CAP_TINH - Đau bụng cấp tính dữ dội' },
  { value: 'TAC_RUOT', label: 'TAC_RUOT - Tắc ruột (không thể trung tiện/đại tiện)' },
  { value: 'KHONG_THE_DI_TIEU', label: 'KHONG_THE_DI_TIEU - Không thể đi tiểu / bí tiểu' },
  { value: 'SOC_MAT_MAU', label: 'SOC_MAT_MAU - Sốc mất máu / mất nước / ngất xỉu' },
  { value: 'NHIEM_TRUNG_NANG', label: 'NHIEM_TRUNG_NANG - Nhiễm trùng nặng / nghi ruột thừa' },
  { value: 'DAU_LAN_CO_QUAN_KHAC', label: 'DAU_LAN_CO_QUAN_KHAC - Đau lan sang lưng / vai / ngực' },
  { value: 'KEM_HO_HAP_TIM_MACH', label: 'KEM_HO_HAP_TIM_MACH - Kèm khó thở / đau ngực' },
  { value: 'TIEU_DUONG_KEM_NON', label: 'TIEU_DUONG_KEM_NON - Bệnh tiểu đường kèm nôn' },
  { value: 'VANG_DA_KEM_DAU_BUNG', label: 'VANG_DA_KEM_DAU_BUNG - Vàng da kèm đau bụng' },
  { value: 'MAT_NUOC_NANG', label: 'MAT_NUOC_NANG - Mất nước nặng do tiêu chảy kéo dài' },
  { value: 'SUT_CAN_KHONG_RO_NGUYEN_NHAN', label: 'SUT_CAN_KHONG_RO_NGUYEN_NHAN - Sụt cân không rõ nguyên nhân' },
  { value: 'KHO_NUOT_DAU_KHI_NUOT', label: 'KHO_NUOT_DAU_KHI_NUOT - Khó / đau rát khi nuốt' },
]

export interface StandardCatalogCodeMeta {
  code: string
  name: string
  source: string
  sourceType: 'icd' | 'triage' | 'drug' | 'custom'
  desc: string
  scope: string
  defaultTriageCode?: string
}

const CUSTOM_CODES_STORAGE_KEY = 'gastroai_custom_catalog_code_metas'

function loadCustomCodeMetas(): Record<string, StandardCatalogCodeMeta & { type: CatalogType }> {
  try {
    const raw = localStorage.getItem(CUSTOM_CODES_STORAGE_KEY)
    if (!raw) return {}
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

function saveCustomCodeMetas(metas: Record<string, StandardCatalogCodeMeta & { type: CatalogType }>) {
  try {
    localStorage.setItem(CUSTOM_CODES_STORAGE_KEY, JSON.stringify(metas))
  } catch (err) {
    console.error('Failed to save custom catalog code metas', err)
  }
}

// Bộ từ điển mã chuẩn hóa y khoa tiêu hóa phân loại theo Triệu chứng, Bệnh lý và Thuốc
const STANDARD_CODES_BY_TYPE: Record<CatalogType, StandardCatalogCodeMeta[]> = {
  SYMPTOM: [
    { code: 'ABDOMINAL_PAIN', name: 'Đau bụng', source: 'ICD-10: R10 / NHS', sourceType: 'triage', desc: 'Đau bụng cấp tính hoặc quặn từng cơn', scope: 'Triage tầng 1 & Đánh giá triệu chứng', defaultTriageCode: 'ABDOMINAL_PAIN' },
    { code: 'BLOATING', name: 'Đầy hơi, chướng bụng', source: 'ICD-10: R14', sourceType: 'icd', desc: 'Cảm giác căng trướng, đầy hơi dạ dày ruột', scope: 'Hồ sơ bệnh án & Triage', defaultTriageCode: 'BLOATING' },
    { code: 'CONSTIPATION', name: 'Táo bón', source: 'ICD-10: K59.0', sourceType: 'icd', desc: 'Táo bón khó đi tiêu, phân khô cứng', scope: 'Nhật ký Bristol & Hồ sơ bệnh án', defaultTriageCode: 'CONSTIPATION' },
    { code: 'DIARRHEA', name: 'Tiêu chảy', source: 'ICD-10: K52.9', sourceType: 'triage', desc: 'Đi ngoài phân lỏng nhiều lần trong ngày', scope: 'Nhật ký Bristol & Cảnh báo mất nước', defaultTriageCode: 'DIARRHEA' },
    { code: 'HEARTBURN', name: 'Ợ nóng, trào ngược', source: 'ICD-10: R12', sourceType: 'icd', desc: 'Ợ nóng, nóng rát vùng sau xương ức', scope: 'Sàng lọc trào ngược & Hồ sơ bệnh án', defaultTriageCode: 'HEARTBURN' },
    { code: 'NAUSEA', name: 'Buồn nôn', source: 'ICD-10: R11.0', sourceType: 'icd', desc: 'Cảm giác buồn nôn, cồn cào khó chịu', scope: 'Đánh giá triệu chứng', defaultTriageCode: 'NAUSEA' },
    { code: 'VOMITING', name: 'Nôn', source: 'ICD-10: R11.1', sourceType: 'triage', desc: 'Nôn mửa tống xuất thức ăn qua miệng', scope: 'Cảnh báo mất nước & Triage', defaultTriageCode: 'VOMITING' },
    { code: 'BLACK_TARRY_STOOL', name: 'Phân đen như hắc ín', source: 'Cấp cứu 115 (Mayo Clinic)', sourceType: 'triage', desc: 'Phân đen bóng như hắc ín/bã cà phê - Dấu hiệu xuất huyết tiêu hóa trên', scope: 'Kích hoạt báo động đỏ 115', defaultTriageCode: 'BLACK_OR_BLOODY_STOOL' },
    { code: 'FRESH_RED_BLOOD_IN_STOOL', name: 'Máu đỏ tươi trong phân', source: 'Cấp cứu 115 (NHS 111)', sourceType: 'triage', desc: 'Máu tươi lẫn trong phân hoặc chảy nhỏ giọt sau đại tiện', scope: 'Kích hoạt báo động đỏ 115', defaultTriageCode: 'BLACK_OR_BLOODY_STOOL' },
    { code: 'BLOOD_IN_VOMIT', name: 'Nôn ra máu hoặc chất nôn như bã cà phê', source: 'Cấp cứu 115 (Cleveland Clinic)', sourceType: 'triage', desc: 'Nôn ra máu đỏ tươi hoặc dịch sẫm màu bã cà phê', scope: 'Kích hoạt báo động đỏ 115', defaultTriageCode: 'BLOOD_IN_VOMIT' },
    { code: 'EPIGASTRIC_PAIN', name: 'Đau thắt vùng thượng vị', source: 'ICD-10: R10.1', sourceType: 'icd', desc: 'Đau tức, nóng rát vùng thượng vị (trên rốn)', scope: 'Sàng lọc dạ dày tá tràng', defaultTriageCode: 'ABDOMINAL_PAIN' },
    { code: 'INDIGESTION', name: 'Khó tiêu / Đầy bụng khó tiêu', source: 'ICD-10: K30', sourceType: 'icd', desc: 'Khó tiêu chức năng, ăn mau no, trướng bụng', scope: 'Hồ sơ bệnh án & Khẩu phần ăn' },
    { code: 'LOSS_OF_APPETITE', name: 'Chán ăn, ăn không ngon miệng', source: 'ICD-10: R63.0', sourceType: 'icd', desc: 'Giảm cảm giác thèm ăn kéo dài', scope: 'Theo dõi dinh dưỡng' },
    { code: 'DYSPHAGIA', name: 'Khó nuốt / Nuốt nghẹn', source: 'Báo động đỏ (NHS)', sourceType: 'triage', desc: 'Khó nuốt, nuốt vướng nghẹn thức ăn hoặc chất lỏng', scope: 'Cảnh báo nguy cơ tổn thương thực quản', defaultTriageCode: 'KHO_NUOT_DAU_KHI_NUOT' },
    { code: 'ODYNOPHAGIA', name: 'Đau khi nuốt', source: 'Lâm sàng Tiêu hóa', sourceType: 'triage', desc: 'Đau rát khi nuốt thức ăn qua ngực', scope: 'Cảnh báo viêm loét thực quản', defaultTriageCode: 'KHO_NUOT_DAU_KHI_NUOT' },
    { code: 'JAUNDICE', name: 'Vàng da, vàng mắt', source: 'Báo động đỏ (Mayo Clinic)', sourceType: 'triage', desc: 'Vàng da, vàng củng mạc mắt kèm đau bụng', scope: 'Cảnh báo bệnh lý gan mật tụy', defaultTriageCode: 'VANG_DA_KEM_DAU_BUNG' },
    { code: 'UNEXPLAINED_WEIGHT_LOSS', name: 'Sụt cân không rõ nguyên nhân', source: 'Báo động đỏ (NHS/Cleveland)', sourceType: 'triage', desc: 'Sụt >5% cân nặng trong 6 tháng không chủ ý', scope: 'Cảnh báo ung thư đường tiêu hóa', defaultTriageCode: 'SUT_CAN_KHONG_RO_NGUYEN_NHAN' },
    { code: 'BELCHING', name: 'Ợ hơi, ợ chua', source: 'ICD-10: R14', sourceType: 'icd', desc: 'Ợ hơi liên tục sau ăn hoặc lúc đói', scope: 'Theo dõi trào ngược GERD', defaultTriageCode: 'HEARTBURN' },
    { code: 'TENESMUS', name: 'Mót rặn / Đi ngoài không hết', source: 'ICD-10: R19.8', sourceType: 'icd', desc: 'Cảm giác mót rặn đại tiện liên tục nhưng đi ít', scope: 'Theo dõi viêm đại trực tràng' },
    { code: 'FEVER', name: 'Sốt cao kèm đau bụng', source: 'Cấp cứu Lâm sàng', sourceType: 'triage', desc: 'Sốt >38.5°C kèm đau bụng cấp', scope: 'Cảnh báo viêm ruột thừa / nhiễm trùng nặng', defaultTriageCode: 'NHIEM_TRUNG_NANG' },
    { code: 'FATIGUE', name: 'Mệt mỏi, suy nhược', source: 'ICD-10: R53', sourceType: 'icd', desc: 'Mệt mỏi do thiếu máu hoặc kém hấp thu', scope: 'Theo dõi thể trạng' },
  ],
  CONDITION: [
    { code: 'GERD', name: 'Trào ngược dạ dày thực quản', source: 'ICD-10: K21', sourceType: 'icd', desc: 'Trào ngược dịch vị dạ dày lên thực quản gây viêm/ợ nóng', scope: 'Hồ sơ bệnh án & Bài viết y khoa' },
    { code: 'GASTRITIS', name: 'Viêm dạ dày', source: 'ICD-10: K29.7', sourceType: 'icd', desc: 'Viêm niêm mạc dạ dày cấp hoặc mạn tính', scope: 'Hồ sơ bệnh án & Phân loại bài viết' },
    { code: 'PEPTIC_ULCER', name: 'Loét dạ dày tá tràng', source: 'ICD-10: K27', sourceType: 'icd', desc: 'Loét niêm mạc dạ dày hoặc hành tá tràng', scope: 'Hồ sơ bệnh án & Cảnh báo dùng thuốc' },
    { code: 'IRRITABLE_BOWEL_SYNDROME', name: 'Hội chứng ruột kích thích', source: 'ICD-10: K58 (Rome IV)', sourceType: 'icd', desc: 'Hội chứng ruột kích thích (IBS) - Rối loạn trục não ruột', scope: 'Nhật ký Bristol & Chế độ ăn Low-FODMAP' },
    { code: 'INFLAMMATORY_BOWEL_DISEASE', name: 'Bệnh viêm ruột', source: 'ICD-10: K50/K51', sourceType: 'icd', desc: 'Bệnh viêm ruột tự miễn mạn tính (Crohn hoặc Viêm loét đại tràng)', scope: 'Hồ sơ bệnh án chuyên sâu' },
    { code: 'CELIAC_DISEASE', name: 'Bệnh Celiac', source: 'ICD-10: K90.0', sourceType: 'icd', desc: 'Bệnh không dung nạp Gluten gây tổn thương nhung mao ruột', scope: 'Hồ sơ bệnh án & Cảnh báo dị ứng' },
    { code: 'HELICOBACTER_PYLORI', name: 'Nhiễm Helicobacter pylori (H. pylori)', source: 'ICD-10: B98.0', sourceType: 'icd', desc: 'Nhiễm vi khuẩn HP dạ dày gây viêm loét', scope: 'Hồ sơ bệnh án & Đơn thuốc tiệt trừ HP' },
    { code: 'HEMORRHOIDS', name: 'Trĩ', source: 'ICD-10: K64', sourceType: 'icd', desc: 'Bệnh trĩ nội, trĩ ngoại vùng hậu môn trực tràng', scope: 'Hồ sơ bệnh án & Theo dõi đại tiện' },
    { code: 'HEPATITIS_B', name: 'Viêm gan B', source: 'ICD-10: B18.1', sourceType: 'icd', desc: 'Viêm gan virus B mạn tính', scope: 'Hồ sơ tiền sử bệnh lý' },
    { code: 'HEPATITIS_C', name: 'Viêm gan C', source: 'ICD-10: B18.2', sourceType: 'icd', desc: 'Viêm gan virus C mạn tính', scope: 'Hồ sơ tiền sử bệnh lý' },
    { code: 'FATTY_LIVER', name: 'Gan nhiễm mỡ', source: 'ICD-10: K76.0', sourceType: 'icd', desc: 'Thoái hóa mỡ ở gan (NAFLD/MASLD)', scope: 'Hồ sơ bệnh án & Chế độ ăn uống' },
    { code: 'GALLSTONES', name: 'Sỏi túi mật', source: 'ICD-10: K80', sourceType: 'icd', desc: 'Sỏi trong túi mật hoặc đường mật', scope: 'Hồ sơ bệnh án & Triage đau bụng' },
    { code: 'COLITIS', name: 'Viêm đại tràng', source: 'ICD-10: K52.9', sourceType: 'icd', desc: 'Viêm đại tràng cấp tính hoặc mạn tính', scope: 'Hồ sơ bệnh án & Nhật ký ăn uống' },
    { code: 'PANCREATITIS', name: 'Viêm tụy', source: 'ICD-10: K85', sourceType: 'triage', desc: 'Viêm tụy cấp/mạn - Cần theo dõi khẩn cấp', scope: 'Hồ sơ bệnh án & Cảnh báo cấp cứu' },
    { code: 'GASTRIC_POLYP', name: 'Polyp dạ dày', source: 'ICD-10: D12.0', sourceType: 'icd', desc: 'Polyp niêm mạc dạ dày qua nội soi', scope: 'Hồ sơ nội soi & Tiền sử' },
    { code: 'COLON_POLYP', name: 'Polyp đại trực tràng', source: 'ICD-10: D12.6', sourceType: 'icd', desc: 'Polyp đại trực tràng cần tầm soát ung thư sớm', scope: 'Hồ sơ nội soi & Tầm soát ung thư' },
    { code: 'CONSTIPATION_CHRONIC', name: 'Táo bón mạn tính', source: 'ICD-10: K59.0', sourceType: 'icd', desc: 'Táo bón vô căn kéo dài', scope: 'Theo dõi phân Bristol & Chế độ ăn' },
  ],
  MEDICATION: [
    { code: 'OMEPRAZOLE', name: 'Omeprazole', source: 'Dược thư QG / INN: PPI', sourceType: 'drug', desc: 'Ức chế bơm proton thế hệ 1, giảm tiết axit dịch vị', scope: 'Hồ sơ dùng thuốc & Nhắc uống thuốc' },
    { code: 'ESOMEPRAZOLE', name: 'Esomeprazole', source: 'Dược thư QG / Nexium', sourceType: 'drug', desc: 'Đồng phân S của omeprazole, ức chế axit mạnh và kéo dài', scope: 'Hồ sơ dùng thuốc & Nhắc uống thuốc' },
    { code: 'PANTOPRAZOLE', name: 'Pantoprazole', source: 'Dược thư QG / INN: PPI', sourceType: 'drug', desc: 'Ức chế bơm proton, ít tương tác thuốc qua CYP2C19', scope: 'Hồ sơ dùng thuốc & Nhắc uống thuốc' },
    { code: 'LANSOPRAZOLE', name: 'Lansoprazole', source: 'Dược thư QG / INN: PPI', sourceType: 'drug', desc: 'Thuốc nhóm PPI trị viêm loét dạ dày - tá tràng', scope: 'Nhắc uống thuốc' },
    { code: 'RABEPRAZOLE', name: 'Rabeprazole', source: 'Dược thư QG / Pariet', sourceType: 'drug', desc: 'PPI ức chế bơm proton khởi phát tác dụng nhanh', scope: 'Nhắc uống thuốc' },
    { code: 'FAMOTIDINE', name: 'Famotidine', source: 'Dược thư QG / Kháng H2', sourceType: 'drug', desc: 'Kháng thụ thể Histamin H2 ở tế bào viền dạ dày', scope: 'Nhắc uống thuốc' },
    { code: 'CIMETIDINE', name: 'Cimetidine', source: 'Dược thư QG / Kháng H2', sourceType: 'drug', desc: 'Thuốc kháng H2 cổ điển giảm tiết acid', scope: 'Nhắc uống thuốc' },
    { code: 'ANTACID', name: 'Thuốc kháng acid', source: 'Dược thư QG / Kháng acid', sourceType: 'drug', desc: 'Chứa nhôm/magnesi hydroxyd trung hòa acid tại chỗ (Maalox/Gastropulgite)', scope: 'Đơn thuốc & Dặn dò sau ăn' },
    { code: 'SIMETHICONE', name: 'Simethicone', source: 'Dược thư QG / Phá bọt khí', sourceType: 'drug', desc: 'Giảm sức căng bề mặt bọt khí trong dạ dày ruột, trị đầy hơi', scope: 'Đơn thuốc trị đầy bụng' },
    { code: 'PSYLLIUM', name: 'Psyllium (chất xơ hòa tan)', source: 'Chất xơ tự nhiên', sourceType: 'drug', desc: 'Vỏ hạt mã đề hút nước tạo khối phân mềm trị táo bón', scope: 'Theo dõi táo bón & Dinh dưỡng' },
    { code: 'LOPERAMIDE', name: 'Loperamide', source: 'Dược thư QG / Cầm tiêu chảy', sourceType: 'drug', desc: 'Giảm nhu động ruột, kéo dài thời gian lưu phân', scope: 'Cảnh báo dùng thuốc cầm tiêu chảy' },
    { code: 'BISMUTH_SUBSALICYLATE', name: 'Bismuth subsalicylate', source: 'Dược thư QG / Pepto', sourceType: 'drug', desc: 'Bảo vệ niêm mạc, kháng khuẩn H. pylori, trị tiêu chảy nhẹ', scope: 'Phác đồ điều trị dạ dày' },
    { code: 'PROBIOTIC', name: 'Men vi sinh (probiotic)', source: 'Lợi khuẩn đường ruột', sourceType: 'drug', desc: 'Bổ sung lợi khuẩn Lactobacillus/Bifidobacterium cân bằng hệ vi sinh', scope: 'Hỗ trợ tiêu hóa & Dùng cùng kháng sinh' },
    { code: 'DOMPERIDONE', name: 'Domperidone', source: 'Dược thư QG / Chống nôn', sourceType: 'drug', desc: 'Đối kháng thụ thể dopamin, tăng tốc độ làm rỗng dạ dày', scope: 'Đơn thuốc chống trào ngược / nôn' },
    { code: 'METOCLOPRAMIDE', name: 'Metoclopramide', source: 'Dược thư QG / Chống nôn', sourceType: 'drug', desc: 'Kích thích nhu động đường tiêu hóa trên', scope: 'Nhắc uống thuốc' },
    { code: 'DROTAVERINE', name: 'Drotaverine (No-spa)', source: 'Dược thư QG / No-spa', sourceType: 'drug', desc: 'Giảm co thắt cơ trơn tiêu hóa, giảm đau quặn bụng', scope: 'Đơn thuốc giảm đau co thắt' },
    { code: 'MEBEVERINE', name: 'Mebeverine', source: 'Dược thư QG / Chống co thắt', sourceType: 'drug', desc: 'Chống co thắt cơ trơn đại tràng trong hội chứng ruột kích thích', scope: 'Đơn thuốc điều trị IBS' },
  ],
}

const TYPE_CONFIG: Record<
  CatalogType,
  { label: string; color: string; badgeColor: string; icon: React.ReactNode }
> = {
  SYMPTOM: {
    label: 'Triệu chứng',
    color: 'cyan',
    badgeColor: '#13c2c2',
    icon: <ExperimentOutlined />,
  },
  CONDITION: {
    label: 'Bệnh lý',
    color: 'orange',
    badgeColor: '#fa8c16',
    icon: <ThunderboltOutlined />,
  },
  MEDICATION: {
    label: 'Thuốc',
    color: 'green',
    badgeColor: '#52c41a',
    icon: <MedicineBoxOutlined />,
  },
}

export default function CmsCatalogsPage() {
  const [items, setItems] = useState<CmsCatalogItem[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [selectedTypeTab, setSelectedTypeTab] = useState<string>('ALL')
  const [searchText, setSearchText] = useState('')
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')

  // Modal create/edit
  const [modalOpen, setModalOpen] = useState(false)
  const [guideModalOpen, setGuideModalOpen] = useState(false)
  const [defineCodeModalOpen, setDefineCodeModalOpen] = useState(false)
  const [customMetas, setCustomMetas] = useState<Record<string, StandardCatalogCodeMeta & { type: CatalogType }>>(() => loadCustomCodeMetas())
  const [editingItem, setEditingItem] = useState<CmsCatalogItem | null>(null)
  const [modalSubmitting, setModalSubmitting] = useState(false)
  const [modalError, setModalError] = useState<string | null>(null)
  const [form] = Form.useForm<CmsCatalogItemRequest>()
  const [defineCodeForm] = Form.useForm<{
    type: CatalogType
    code: string
    name: string
    source: string
    sourceType: 'icd' | 'triage' | 'drug' | 'custom'
    desc: string
    scope: string
    defaultTriageCode?: string
  }>()

  const watchedType = Form.useWatch('type', form)
  const watchedCode = Form.useWatch('code', form)
  const [codeSearchText, setCodeSearchText] = useState('')
  const [triageSearchText, setTriageSearchText] = useState('')

  const defineWatchedCode = Form.useWatch('code', defineCodeForm)
  const defineWatchedName = Form.useWatch('name', defineCodeForm)
  const defineWatchedSource = Form.useWatch('source', defineCodeForm)
  const defineWatchedSourceType = Form.useWatch('sourceType', defineCodeForm)
  const defineWatchedDesc = Form.useWatch('desc', defineCodeForm)
  const defineWatchedScope = Form.useWatch('scope', defineCodeForm)

  const activeModalType: CatalogType = (watchedType ||
    (selectedTypeTab === 'ALL' ? 'SYMPTOM' : selectedTypeTab)) as CatalogType

  const selectedCodeMeta = useMemo(() => {
    if (!watchedCode) return null
    if (customMetas[watchedCode]) return customMetas[watchedCode]
    const list = STANDARD_CODES_BY_TYPE[activeModalType] || []
    return list.find((it) => it.code === watchedCode) || null
  }, [watchedCode, activeModalType, customMetas])

  const codeOptions = useMemo(() => {
    const baseList = STANDARD_CODES_BY_TYPE[activeModalType] || []
    const customList = Object.values(customMetas).filter((m) => m.type === activeModalType)
    const combinedList = [...baseList, ...customList]
    const existingForType = items.filter((it) => it.type === activeModalType)

    const map = new Map<
      string,
      { value: string; label: React.ReactNode; searchText: string }
    >()

    combinedList.forEach((it) => {
      const tagColor =
        it.sourceType === 'triage'
          ? 'volcano'
          : it.sourceType === 'drug'
            ? 'green'
            : it.sourceType === 'custom'
              ? 'purple'
              : 'blue'
      map.set(it.code, {
        value: it.code,
        searchText: `${it.code} ${it.name} ${it.source}`,
        label: (
          <div className="flex items-center justify-between py-0.5">
            <div className="flex items-center gap-1.5 overflow-hidden">
              <span className="font-mono font-semibold text-slate-800 dark:text-slate-100 shrink-0">
                {it.code}
              </span>
              <span className="text-slate-500 dark:text-slate-400 text-xs truncate">
                · {it.name}
              </span>
            </div>
            <Tag color={tagColor} className="text-[10px] ml-2 shrink-0 border-0">
              {it.source}
            </Tag>
          </div>
        ),
      })
    })

    existingForType.forEach((it) => {
      if (!map.has(it.code)) {
        map.set(it.code, {
          value: it.code,
          searchText: `${it.code} ${it.name}`,
          label: (
            <div className="flex items-center justify-between py-0.5">
              <div className="flex items-center gap-1.5 overflow-hidden">
                <span className="font-mono font-semibold text-slate-800 dark:text-slate-100 shrink-0">
                  {it.code}
                </span>
                <span className="text-slate-500 dark:text-slate-400 text-xs truncate">
                  · {it.name}
                </span>
              </div>
              <Tag className="text-[10px] ml-2 shrink-0">Hệ thống</Tag>
            </div>
          ),
        })
      }
    })

    if (editingItem && editingItem.type === activeModalType && !map.has(editingItem.code)) {
      map.set(editingItem.code, {
        value: editingItem.code,
        searchText: `${editingItem.code} ${editingItem.name}`,
        label: (
          <div className="flex items-center justify-between py-0.5">
            <span className="font-mono font-semibold text-slate-800 dark:text-slate-100">
              {editingItem.code}
            </span>
            <Tag className="text-[10px] ml-2 shrink-0">Hiện tại</Tag>
          </div>
        ),
      })
    }

    if (watchedCode && !map.has(watchedCode)) {
      map.set(watchedCode, {
        value: watchedCode,
        searchText: watchedCode,
        label: (
          <div className="flex items-center justify-between py-0.5">
            <span className="font-mono font-semibold text-emerald-600">
              {watchedCode}
            </span>
            <Tag color="cyan" className="text-[10px] ml-2 shrink-0">
              Mã mới tạo
            </Tag>
          </div>
        ),
      })
    }

    if (codeSearchText.trim()) {
      const customCode = codeSearchText
        .trim()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/gi, 'd')
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '')
      if (customCode && !map.has(customCode)) {
        map.set(customCode, {
          value: customCode,
          searchText: `${customCode} ${codeSearchText}`,
          label: (
            <div className="flex items-center justify-between py-0.5">
              <span className="font-mono font-semibold text-emerald-600">
                + Dùng mã mới: {customCode}
              </span>
              <Tag color="cyan" className="text-[10px] ml-2 shrink-0">
                Mã tùy chỉnh
              </Tag>
            </div>
          ),
        })
      }
    }

    return Array.from(map.values())
  }, [activeModalType, items, editingItem, watchedCode, codeSearchText, customMetas])

  const triageOptions = useMemo(() => {
    const map = new Map<string, string>()
    TRIAGE_OPTIONS.forEach((it) => map.set(it.value, it.label))

    items.forEach((it) => {
      if (it.triageCode && !map.has(it.triageCode)) {
        map.set(it.triageCode, `${it.triageCode} (Đang dùng trong hệ thống)`)
      }
    })

    if (editingItem?.triageCode && !map.has(editingItem.triageCode)) {
      map.set(editingItem.triageCode, editingItem.triageCode)
    }

    if (triageSearchText.trim()) {
      const customTriage = triageSearchText.trim().toUpperCase().replace(/\s+/g, '_')
      if (!map.has(customTriage)) {
        map.set(customTriage, `${customTriage} (Mã Triage tùy chỉnh)`)
      }
    }

    return Array.from(map.entries()).map(([value, label]) => ({
      value,
      label,
    }))
  }, [items, editingItem, triageSearchText])

  const deleteGuard = useInFlightGuard<number>()
  const toggleGuard = useInFlightGuard<number>()

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listCmsCatalogs()
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách danh mục.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Statistics calculation
  const stats = useMemo(() => {
    const total = items.length
    const symptoms = items.filter((i) => i.type === 'SYMPTOM').length
    const conditions = items.filter((i) => i.type === 'CONDITION').length
    const medications = items.filter((i) => i.type === 'MEDICATION').length
    const activeCount = items.filter((i) => i.active).length
    return { total, symptoms, conditions, medications, activeCount }
  }, [items])

  // Filtered items
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      // Type tab filter
      if (selectedTypeTab !== 'ALL' && item.type !== selectedTypeTab) {
        return false
      }
      // Active filter
      if (activeFilter === 'ACTIVE' && !item.active) return false
      if (activeFilter === 'INACTIVE' && item.active) return false

      // Search text
      if (searchText.trim()) {
        const q = searchText.trim().toLowerCase()
        const matchName = item.name.toLowerCase().includes(q)
        const matchCode = item.code.toLowerCase().includes(q)
        const matchTriage = item.triageCode ? item.triageCode.toLowerCase().includes(q) : false
        if (!matchName && !matchCode && !matchTriage) {
          return false
        }
      }

      return true
    })
  }, [items, selectedTypeTab, activeFilter, searchText])

  // Handlers for modal
  function handleOpenCreate() {
    setEditingItem(null)
    setModalError(null)
    setCodeSearchText('')
    setTriageSearchText('')
    const currentTabType = selectedTypeTab !== 'ALL' ? (selectedTypeTab as CatalogType) : 'SYMPTOM'
    // Default next sortOrder based on selected type
    const maxSort = items
      .filter((i) => i.type === currentTabType)
      .reduce((max, i) => Math.max(max, i.sortOrder || 0), 0)

    form.resetFields()
    form.setFieldsValue({
      type: currentTabType,
      code: '',
      name: '',
      triageCode: null,
      sortOrder: maxSort + 10,
      active: true,
    })
    setModalOpen(true)
  }

  function handleOpenEdit(record: CmsCatalogItem) {
    setEditingItem(record)
    setModalError(null)
    setCodeSearchText('')
    setTriageSearchText('')
    form.resetFields()
    form.setFieldsValue({
      type: record.type,
      code: record.code,
      name: record.name,
      triageCode: record.triageCode,
      sortOrder: record.sortOrder,
      active: record.active,
    })
    setModalOpen(true)
  }

  function handleOpenDefineCodeModal() {
    const curType = form.getFieldValue('type') || activeModalType
    const curName = form.getFieldValue('name') || ''
    const generatedCode = curName
      ? curName
          .trim()
          .normalize('NFD')
          .replace(/[\u0300-\u036f]/g, '')
          .replace(/đ/gi, 'd')
          .toUpperCase()
          .replace(/[^A-Z0-9]+/g, '_')
          .replace(/^_+|_+$/g, '')
      : ''

    defineCodeForm.resetFields()
    defineCodeForm.setFieldsValue({
      type: curType,
      code: generatedCode,
      name: curName,
      source: curType === 'CONDITION' ? 'ICD-10: ' : curType === 'MEDICATION' ? 'Dược thư QG: ' : 'Mayo/NHS: ',
      sourceType: curType === 'CONDITION' ? 'icd' : curType === 'MEDICATION' ? 'drug' : 'triage',
      desc: '',
      scope:
        curType === 'CONDITION'
          ? 'Hồ sơ bệnh án & Bài viết y khoa'
          : curType === 'MEDICATION'
            ? 'Hồ sơ dùng thuốc & Nhắc uống thuốc'
            : 'Triage tầng 1 & Cảnh báo khẩn cấp 115',
    })
    setDefineCodeModalOpen(true)
  }

  function handleSaveDefinedCode() {
    defineCodeForm.validateFields().then((values) => {
      const normalizedCode = values.code.trim().toUpperCase().replace(/\s+/g, '_')
      const newMeta: StandardCatalogCodeMeta & { type: CatalogType } = {
        code: normalizedCode,
        name: values.name.trim(),
        source: values.source.trim(),
        sourceType: values.sourceType,
        desc: values.desc.trim(),
        scope: values.scope.trim(),
        defaultTriageCode: values.defaultTriageCode || undefined,
        type: values.type,
      }

      const nextCustomMetas = {
        ...customMetas,
        [normalizedCode]: newMeta,
      }
      setCustomMetas(nextCustomMetas)
      saveCustomCodeMetas(nextCustomMetas)

      // Apply directly to parent modal form
      form.setFieldValue('type', values.type)
      form.setFieldValue('code', normalizedCode)
      if (values.name && (!form.getFieldValue('name') || !editingItem)) {
        form.setFieldValue('name', values.name.trim())
      }
      if (values.defaultTriageCode && !form.getFieldValue('triageCode')) {
        form.setFieldValue('triageCode', values.defaultTriageCode)
      }

      notification.success({
        message: 'Đã thêm mã chuẩn hóa mới',
        description: `Mã "${normalizedCode}" kèm tag [${values.source}] đã được lưu vào từ điển và chọn vào biểu mẫu.`,
      })
      setDefineCodeModalOpen(false)
    })
  }

  async function handleModalSubmit() {
    try {
      const values = await form.validateFields()
      setModalSubmitting(true)
      setModalError(null)

      const payload: CmsCatalogItemRequest = {
        ...values,
        code: values.code.trim().toUpperCase().replace(/\s+/g, '_'),
        name: values.name.trim(),
        triageCode: values.triageCode ? values.triageCode.trim().toUpperCase().replace(/\s+/g, '_') : null,
      }

      if (editingItem) {
        await updateCmsCatalog(editingItem.id, payload)
        notification.success({
          message: 'Cập nhật thành công',
          description: `Đã lưu thay đổi cho danh mục "${payload.name}".`,
        })
      } else {
        await createCmsCatalog(payload)
        notification.success({
          message: 'Thêm mới thành công',
          description: `Đã thêm danh mục "${payload.name}" (${payload.code}).`,
        })
      }

      setModalOpen(false)
      loadData()
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        return // Ant Design form validation error
      }
      setModalError(err instanceof Error ? err.message : 'Đã có lỗi xảy ra khi lưu danh mục.')
    } finally {
      setModalSubmitting(false)
    }
  }

  async function handleToggle(record: CmsCatalogItem) {
    await toggleGuard.run(record.id, async () => {
      try {
        const updated = await toggleCmsCatalogActive(record.id)
        setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
        notification.success({
          message: updated.active ? 'Đã kích hoạt' : 'Đã tắt kích hoạt',
          description: `Danh mục "${updated.name}" hiện ${updated.active ? 'sẽ xuất hiện' : 'sẽ ẩn'} trên hệ thống.`,
        })
      } catch (err) {
        notification.error({
          message: 'Thao tác thất bại',
          description: err instanceof Error ? err.message : 'Không thể thay đổi trạng thái danh mục.',
        })
      }
    })
  }

  async function handleDelete(record: CmsCatalogItem) {
    await deleteGuard.run(record.id, async () => {
      try {
        await deleteCmsCatalog(record.id)
        setItems((prev) => prev.filter((item) => item.id !== record.id))
        notification.success({
          message: 'Xóa thành công',
          description: `Đã xóa danh mục "${record.name}".`,
        })
      } catch (err) {
        notification.error({
          message: 'Xóa thất bại',
          description: err instanceof Error ? err.message : 'Không thể xóa danh mục này.',
        })
      }
    })
  }

  const columns: ColumnsType<CmsCatalogItem> = [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: (_val, _record, index) => <span className="text-gray-400 font-mono text-xs">{index + 1}</span>,
    },
    {
      title: 'Phân loại',
      dataIndex: 'type',
      key: 'type',
      width: 140,
      render: (type: CatalogType) => {
        const cfg = TYPE_CONFIG[type]
        return (
          <Tag color={cfg.color} className="inline-flex items-center gap-1 font-medium px-2 py-0.5">
            {cfg.icon}
            <span>{cfg.label}</span>
          </Tag>
        )
      },
    },
    {
      title: 'Tên hiển thị & Mã chuẩn hóa',
      dataIndex: 'name',
      key: 'name',
      width: 280,
      render: (name: string, record) => {
        const meta =
          customMetas[record.code] ||
          (STANDARD_CODES_BY_TYPE[record.type] || []).find((it) => it.code === record.code)
        return (
          <div className="flex flex-col">
            <span className="font-semibold text-slate-800 dark:text-slate-100">{name}</span>
            <div className="flex items-center gap-1.5 mt-0.5">
              <span className="text-xs text-slate-400 font-mono">{record.code}</span>
              {meta?.source && (
                <Tooltip title={`${meta.source}: ${meta.desc} (${meta.scope})`}>
                  <Tag
                    color={
                      meta.sourceType === 'triage'
                        ? 'volcano'
                        : meta.sourceType === 'drug'
                          ? 'green'
                          : meta.sourceType === 'custom'
                            ? 'purple'
                            : 'blue'
                    }
                    className="text-[10px] leading-tight px-1.5 py-0 cursor-help border-0"
                  >
                    {meta.source}
                  </Tag>
                </Tooltip>
              )}
            </div>
          </div>
        )
      },
    },
    {
      title: 'Mã Triage liên kết',
      dataIndex: 'triageCode',
      key: 'triageCode',
      width: 200,
      render: (triageCode: string | null) => {
        if (!triageCode) {
          return <span className="text-slate-300 dark:text-slate-600 italic text-xs">Không có</span>
        }
        return (
          <Tooltip title="Mã nhóm khớp dấu hiệu cảnh báo khẩn cấp trong Triage tầng 1">
            <Tag color="volcano" className="font-mono text-xs font-semibold px-2">
              <ThunderboltOutlined className="mr-1" />
              {triageCode}
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: 'Thứ tự',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 90,
      align: 'center',
      sorter: (a, b) => a.sortOrder - b.sortOrder,
      render: (sortOrder: number) => (
        <span className="inline-block px-2 py-0.5 rounded text-xs font-mono font-medium bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
          {sortOrder}
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 100,
      align: 'center',
      render: (active: boolean, record) => (
        <div className="flex items-center justify-center">
          <Switch
            checked={active}
            size="small"
            loading={toggleGuard.inFlightId === record.id}
            onChange={() => handleToggle(record)}
          />
        </div>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 110,
      align: 'center',
      render: (_val, record) => (
        <Space size="small">
          <Tooltip title="Chỉnh sửa">
            <Button
              size="small"
              type="text"
              icon={<EditOutlined className="text-blue-500" />}
              onClick={() => handleOpenEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="Xác nhận xóa danh mục?"
            description={
              <div className="max-w-xs text-xs">
                Mục <strong className="font-semibold">{record.name}</strong> ({record.code}) sẽ bị xóa vĩnh viễn khỏi
                hệ thống. Nếu chỉ muốn tạm ẩn trong dropdown bệnh nhân, hãy chọn Tắt trạng thái.
              </div>
            }
            okText="Xóa vĩnh viễn"
            cancelText="Hủy"
            okButtonProps={{ danger: true, loading: deleteGuard.inFlightId === record.id }}
            onConfirm={() => handleDelete(record)}
          >
            <Tooltip title="Xóa">
              <Button
                size="small"
                type="text"
                danger
                icon={<DeleteOutlined />}
                loading={deleteGuard.inFlightId === record.id}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-7xl px-4 py-6">
        {/* Header section */}
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <Title level={2} className="!mb-1 tracking-tight flex items-center gap-2">
              <MedicineBoxOutlined className="text-emerald-600" />
              Quản lý danh mục chuẩn hóa
            </Title>
            <Text type="secondary" className="text-sm">
              Chuẩn hóa danh mục triệu chứng, bệnh lý tiêu hóa và thuốc dùng làm nguồn dữ liệu dropdown cho hồ sơ bệnh
              án, nhật ký dùng thuốc và đối soát quy tắc cảnh báo Triage.
            </Text>
          </div>
          <Space>
            <Button icon={<BookOutlined />} onClick={() => setGuideModalOpen(true)}>
              Quy chuẩn mã y khoa
            </Button>
            <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
              Làm mới
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate} className="bg-emerald-600 hover:bg-emerald-500">
              Thêm danh mục mới
            </Button>
          </Space>
        </div>

        {/* Global error banner */}
        {error && (
          <Alert
            message="Không thể kết nối đến máy chủ"
            description={error}
            type="error"
            showIcon
            closable
            className="mb-6"
          />
        )}

        {/* Metric / Stat cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Tổng danh mục
                </Text>
                <div className="text-2xl font-bold text-slate-800 dark:text-slate-100 mt-1">{stats.total}</div>
                <div className="text-xs text-slate-400 mt-0.5">
                  <CheckCircleOutlined className="text-emerald-500 mr-1" />
                  {stats.activeCount} đang kích hoạt
                </div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 flex items-center justify-center text-lg">
                <AppstoreOutlined />
              </div>
            </div>
          </Card>

          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Triệu chứng tiêu hóa
                </Text>
                <div className="text-2xl font-bold text-cyan-600 dark:text-cyan-400 mt-1">{stats.symptoms}</div>
                <div className="text-xs text-slate-400 mt-0.5">Định danh SYMPTOM</div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-cyan-50 dark:bg-cyan-950/40 text-cyan-600 flex items-center justify-center text-lg">
                <ExperimentOutlined />
              </div>
            </div>
          </Card>

          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Bệnh lý tiêu hóa
                </Text>
                <div className="text-2xl font-bold text-orange-600 dark:text-orange-400 mt-1">{stats.conditions}</div>
                <div className="text-xs text-slate-400 mt-0.5">Định danh CONDITION</div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-orange-50 dark:bg-orange-950/40 text-orange-600 flex items-center justify-center text-lg">
                <ThunderboltOutlined />
              </div>
            </div>
          </Card>

          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Thuốc thường dùng
                </Text>
                <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">{stats.medications}</div>
                <div className="text-xs text-slate-400 mt-0.5">Định danh MEDICATION</div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 flex items-center justify-center text-lg">
                <MedicineBoxOutlined />
              </div>
            </div>
          </Card>
        </div>

        {/* Filter and Tab Section */}
        <Card className="shadow-xs border-slate-200 dark:border-slate-800 mb-6" styles={{ body: { padding: '16px 20px' } }}>
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
            <Tabs
              activeKey={selectedTypeTab}
              onChange={setSelectedTypeTab}
              className="!mb-0"
              items={[
                {
                  key: 'ALL',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      Tất cả <Badge count={stats.total} overflowCount={999} color="#64748b" />
                    </span>
                  ),
                },
                {
                  key: 'SYMPTOM',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <ExperimentOutlined /> Triệu chứng <Badge count={stats.symptoms} color="#13c2c2" />
                    </span>
                  ),
                },
                {
                  key: 'CONDITION',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <ThunderboltOutlined /> Bệnh lý <Badge count={stats.conditions} color="#fa8c16" />
                    </span>
                  ),
                },
                {
                  key: 'MEDICATION',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <MedicineBoxOutlined /> Thuốc <Badge count={stats.medications} color="#52c41a" />
                    </span>
                  ),
                },
              ]}
            />

            <div className="flex items-center gap-2.5 flex-nowrap shrink-0">
              <Input
                placeholder="Tìm tên, mã chuẩn hóa, triage..."
                prefix={<SearchOutlined className="text-slate-400" />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                allowClear
                className="w-56 sm:w-64"
              />
              <Select
                value={activeFilter}
                onChange={setActiveFilter}
                className="w-40 shrink-0"
                options={[
                  { value: 'ALL', label: 'Tất cả trạng thái' },
                  { value: 'ACTIVE', label: 'Đang hoạt động' },
                  { value: 'INACTIVE', label: 'Đang ẩn' },
                ]}
              />
            </div>
          </div>
        </Card>

        {/* Data Table */}
        <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: 0 } }}>
          <Table
            columns={columns}
            dataSource={filteredItems}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 15,
              showSizeChanger: true,
              pageSizeOptions: ['15', '30', '50', '100'],
              showTotal: (total, range) => `${range[0]}-${range[1]} của ${total} mục`,
            }}
            locale={{
              emptyText: (
                <div className="py-12 text-center text-slate-400">
                  <FilterOutlined className="text-3xl mb-2" />
                  <div>Không tìm thấy danh mục nào phù hợp</div>
                </div>
              ),
            }}
          />
        </Card>

        {/* Modal Create / Edit */}
        <Modal
          title={
            <div className="flex items-center gap-2 text-base font-semibold">
              <MedicineBoxOutlined className="text-emerald-500" />
              {editingItem ? 'Chỉnh sửa danh mục chuẩn hóa' : 'Thêm danh mục chuẩn hóa mới'}
            </div>
          }
          open={modalOpen}
          onOk={handleModalSubmit}
          confirmLoading={modalSubmitting}
          onCancel={() => {
            setModalOpen(false)
            setCodeSearchText('')
            setTriageSearchText('')
          }}
          okText={editingItem ? 'Lưu thay đổi' : 'Thêm mới'}
          cancelText="Hủy bỏ"
          destroyOnClose
          width={600}
        >
          {modalError && (
            <Alert message="Không thể lưu danh mục" description={modalError} type="error" showIcon className="mb-4" />
          )}

          <Form form={form} layout="vertical" className="mt-4" initialValues={{ sortOrder: 10, active: true }}>
            <Form.Item
              name="type"
              label="Loại danh mục chuẩn hóa"
              rules={[{ required: true, message: 'Vui lòng chọn loại danh mục' }]}
            >
              <Radio.Group
                buttonStyle="solid"
                className="w-full flex"
                onChange={() => {
                  setCodeSearchText('')
                  if (!editingItem) {
                    form.setFieldValue('code', undefined)
                  }
                }}
              >
                <Radio.Button value="SYMPTOM" className="flex-1 text-center">
                  <ExperimentOutlined className="mr-1" /> Triệu chứng
                </Radio.Button>
                <Radio.Button value="CONDITION" className="flex-1 text-center">
                  <ThunderboltOutlined className="mr-1" /> Bệnh lý
                </Radio.Button>
                <Radio.Button value="MEDICATION" className="flex-1 text-center">
                  <MedicineBoxOutlined className="mr-1" /> Thuốc
                </Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Form.Item
              name="name"
              label="Tên hiển thị tiếng Việt"
              rules={[
                { required: true, message: 'Vui lòng nhập tên hiển thị tiếng Việt' },
                { max: 200, message: 'Tên hiển thị tối đa 200 ký tự' },
              ]}
              extra="Tên hiển thị cho bệnh nhân và bác sĩ trong các dropdown chọn lựa."
            >
              <Input placeholder="vd: Đau thắt vùng thượng vị, Omeprazole, Loét dạ dày..." />
            </Form.Item>

            <Form.Item
              name="code"
              label={
                <div className="flex items-center justify-between w-full">
                  <span className="flex items-center gap-1.5 font-medium">
                    Mã chuẩn hóa (Code định danh)
                  </span>
                  <Button
                    type="link"
                    size="small"
                    icon={<PlusCircleOutlined />}
                    className="!p-0 !h-auto text-xs font-semibold text-emerald-600 hover:text-emerald-500"
                    onClick={handleOpenDefineCodeModal}
                  >
                    + Thêm mã chuẩn hóa mới
                  </Button>
                </div>
              }
              rules={[
                { required: true, message: 'Vui lòng chọn hoặc thêm mã chuẩn hóa' },
                { max: 80, message: 'Mã tối đa 80 ký tự' },
              ]}
              extra="Chọn từ danh sách chuẩn y khoa, gõ mã mới trực tiếp hoặc bấm '+ Thêm mã chuẩn hóa mới' bên cạnh."
            >
              <Select
                showSearch
                placeholder="Chọn mã chuẩn hóa (vd: GERD, ABDOMINAL_PAIN...)"
                options={codeOptions}
                searchValue={codeSearchText}
                onSearch={setCodeSearchText}
                filterOption={(input, option) => {
                  const query = input.toLowerCase()
                  const val = String(option?.value ?? '').toLowerCase()
                  const search = String((option as any)?.searchText ?? '').toLowerCase()
                  return val.includes(query) || search.includes(query)
                }}
                onChange={(val) => {
                  setCodeSearchText('')
                  if (val) {
                    const standard = (STANDARD_CODES_BY_TYPE[activeModalType] || []).find((it) => it.code === val)
                    if (standard) {
                      const curName = form.getFieldValue('name')
                      if (!curName || !editingItem) {
                        form.setFieldValue('name', standard.name)
                      }
                      if (standard.defaultTriageCode && !form.getFieldValue('triageCode')) {
                        form.setFieldValue('triageCode', standard.defaultTriageCode)
                      }
                    }
                  }
                }}
                className="font-mono uppercase"
              />
              {selectedCodeMeta && (
                <div className="mt-2.5 p-3 rounded-lg bg-blue-50/70 dark:bg-blue-950/30 border border-blue-200/80 dark:border-blue-900/40 text-xs">
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="font-semibold text-blue-900 dark:text-blue-200 flex items-center gap-1.5">
                      <InfoCircleOutlined className="text-blue-500" />
                      Chuẩn y khoa tham chiếu:
                    </span>
                    <Tag
                      color={
                        selectedCodeMeta.sourceType === 'triage'
                          ? 'volcano'
                          : selectedCodeMeta.sourceType === 'drug'
                            ? 'green'
                            : 'blue'
                      }
                      className="font-medium mr-0"
                    >
                      {selectedCodeMeta.source}
                    </Tag>
                  </div>
                  <div className="text-slate-600 dark:text-slate-300 mb-1 leading-relaxed">
                    <strong className="text-slate-700 dark:text-slate-200">Ý nghĩa: </strong>
                    {selectedCodeMeta.desc}
                  </div>
                  <div className="text-slate-500 dark:text-slate-400 leading-relaxed">
                    <strong className="text-slate-700 dark:text-slate-200">Phạm vi sử dụng: </strong>
                    {selectedCodeMeta.scope}
                  </div>
                </div>
              )}
            </Form.Item>

            <Form.Item
              name="triageCode"
              label={
                <span className="flex items-center gap-1.5">
                  Mã Triage liên kết
                  <Tooltip title="Liên kết triệu chứng với nhóm dấu hiệu khẩn cấp cấp cứu trong Triage tầng 1. Có thể bỏ trống nếu là triệu chứng thông thường hoặc phân loại khác.">
                    <Text type="secondary" className="text-xs font-normal">
                      (Khuyên dùng cho Triệu chứng)
                    </Text>
                  </Tooltip>
                </span>
              }
              extra={
                activeModalType === 'SYMPTOM'
                  ? 'Chọn mã nhóm cấp cứu để kích hoạt cờ cảnh báo Triage (có thể bỏ trống nếu là triệu chứng thông thường).'
                  : 'Không bắt buộc cho bệnh lý và thuốc (có thể để trống).'
              }
            >
              <Select
                showSearch
                allowClear
                placeholder="Chọn mã Triage liên kết (hoặc để trống)"
                options={triageOptions}
                searchValue={triageSearchText}
                onSearch={setTriageSearchText}
                filterOption={(input, option) => {
                  const query = input.toLowerCase()
                  const val = String(option?.value ?? '').toLowerCase()
                  const search = String((option as any)?.searchText ?? '').toLowerCase()
                  return val.includes(query) || search.includes(query)
                }}
                onChange={() => {
                  setTriageSearchText('')
                }}
                className="font-mono"
              />
            </Form.Item>

            <div className="grid grid-cols-2 gap-4">
              <Form.Item
                name="sortOrder"
                label="Thứ tự hiển thị (Sort Order)"
                rules={[{ required: true, message: 'Vui lòng nhập thứ tự' }]}
                extra="Số nhỏ hơn sẽ đứng trước trong danh sách dropdown."
              >
                <InputNumber min={0} step={10} className="w-full" />
              </Form.Item>

              <Form.Item
                name="active"
                label="Trạng thái kích hoạt"
                valuePropName="checked"
                extra="Nếu tắt, mục này sẽ tạm ẩn trong các dropdown người dùng."
              >
                <Switch checkedChildren="Đang dùng" unCheckedChildren="Đang ẩn" />
              </Form.Item>
            </div>
          </Form>
        </Modal>

        {/* Modal Hướng dẫn quy chuẩn mã y khoa */}
        <Modal
          title={
            <div className="flex items-center gap-2 text-base font-semibold">
              <BookOutlined className="text-blue-500" />
              Quy chuẩn Mã định danh (Code) trong Danh mục GastroAI
            </div>
          }
          open={guideModalOpen}
          onCancel={() => setGuideModalOpen(false)}
          footer={[
            <Button key="close" type="primary" onClick={() => setGuideModalOpen(false)}>
              Đã hiểu
            </Button>,
          ]}
          width={700}
        >
          <div className="py-2 space-y-4 text-sm text-slate-600 dark:text-slate-300">
            <div className="p-3.5 bg-blue-50/70 dark:bg-blue-950/30 rounded-lg border border-blue-200/80 dark:border-blue-900/40">
              <div className="font-semibold text-blue-900 dark:text-blue-200 mb-1 flex items-center gap-1.5">
                <InfoCircleOutlined className="text-blue-600" /> Tại sao cần Mã chuẩn hóa?
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed mb-0">
                Tên tiếng Việt có thể linh hoạt thay đổi để người bệnh dễ hiểu, nhưng <strong>Mã chuẩn hóa (Code)</strong> là định danh duy nhất bất biến để liên kết dữ liệu giữa <strong>Hồ sơ bệnh án</strong>, <strong>Nhật ký theo dõi</strong>, <strong>Khớp từ khóa cấp cứu Triage tầng 1</strong> và <strong>Trợ lý AI</strong>.
              </p>
            </div>

            <div className="space-y-3">
              <div className="border border-slate-200 dark:border-slate-800 rounded-lg p-3">
                <div className="flex items-center justify-between mb-1">
                  <span className="font-semibold text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
                    <ExperimentOutlined className="text-cyan-500" /> 1. Triệu chứng tiêu hóa (SYMPTOM)
                  </span>
                  <Tag color="volcano">Cấp cứu Triage & Mayo/NHS</Tag>
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Định danh dựa trên bộ dấu hiệu cảnh báo đỏ khẩn cấp (Red-flag symptoms) của <strong>Mayo Clinic</strong>, <strong>NHS 111</strong> và <strong>Cleveland Clinic</strong>. Mỗi mã triệu chứng nguy hiểm liên kết trực tiếp với mã Triage để kích hoạt cờ cấp cứu (gọi 115) khi phát hiện bệnh nhân có dấu hiệu xuất huyết, sốc hoặc thủng tạng rỗng.
                </div>
              </div>

              <div className="border border-slate-200 dark:border-slate-800 rounded-lg p-3">
                <div className="flex items-center justify-between mb-1">
                  <span className="font-semibold text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
                    <ThunderboltOutlined className="text-orange-500" /> 2. Bệnh lý tiêu hóa (CONDITION)
                  </span>
                  <Tag color="blue">Chuẩn WHO ICD-10</Tag>
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Định danh dựa trên Hệ thống Phân loại Bệnh tật Quốc tế <strong>ICD-10 (Chương XI: K00–K93 - Bệnh hệ tiêu hóa)</strong> và tiêu chuẩn <strong>Rome IV</strong> cho rối loạn chức năng tiêu hóa (như IBS). Mã này làm nguồn dữ liệu dropdown chọn tiền sử bệnh án và gắn thẻ bài viết y khoa.
                </div>
              </div>

              <div className="border border-slate-200 dark:border-slate-800 rounded-lg p-3">
                <div className="flex items-center justify-between mb-1">
                  <span className="font-semibold text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
                    <MedicineBoxOutlined className="text-emerald-500" /> 3. Thuốc thường dùng (MEDICATION)
                  </span>
                  <Tag color="green">Dược thư Quốc gia & INN</Tag>
                </div>
                <div className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Định danh theo Tên gốc hoạt chất quốc tế (<strong>INN</strong>) và <strong>Dược thư Quốc gia Việt Nam</strong> (Bộ Y tế), tập trung vào các nhóm thuốc tiêu hóa chủ lực: Ức chế bơm proton (PPI), Kháng H2, Kháng acid, Men vi sinh và Điều hòa nhu động. Dùng cho tính năng Nhắc uống thuốc và tra cứu phác đồ.
                </div>
              </div>
            </div>
          </div>
        </Modal>

        {/* Modal Định nghĩa mã chuẩn hóa mới */}
        <Modal
          title={
            <div className="flex items-center gap-2 text-base font-semibold">
              <PlusCircleOutlined className="text-emerald-500" />
              Thêm Mã chuẩn hóa mới vào Từ điển y khoa
            </div>
          }
          open={defineCodeModalOpen}
          onOk={handleSaveDefinedCode}
          onCancel={() => setDefineCodeModalOpen(false)}
          okText="Lưu mã vào từ điển & Áp dụng"
          cancelText="Hủy bỏ"
          destroyOnClose
          width={640}
        >
          <div className="mb-4 text-xs text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-900/60 p-3 rounded-lg border border-slate-200 dark:border-slate-800">
            💡 <strong>Mã chuẩn hóa mới</strong> sau khi thêm sẽ có đầy đủ huy hiệu (Tag chuẩn), mô tả ý nghĩa y khoa và phạm vi phục vụ màn hình, xuất hiện trực tiếp trong danh sách chọn của Bác sĩ và bảng quản trị.
          </div>

          <Form form={defineCodeForm} layout="vertical" initialValues={{ sourceType: 'icd' }}>
            <Form.Item
              name="type"
              label="Phân loại danh mục"
              rules={[{ required: true, message: 'Vui lòng chọn phân loại' }]}
            >
              <Radio.Group buttonStyle="solid" className="w-full flex">
                <Radio.Button value="SYMPTOM" className="flex-1 text-center">
                  <ExperimentOutlined className="mr-1" /> Triệu chứng
                </Radio.Button>
                <Radio.Button value="CONDITION" className="flex-1 text-center">
                  <ThunderboltOutlined className="mr-1" /> Bệnh lý
                </Radio.Button>
                <Radio.Button value="MEDICATION" className="flex-1 text-center">
                  <MedicineBoxOutlined className="mr-1" /> Thuốc
                </Radio.Button>
              </Radio.Group>
            </Form.Item>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Form.Item
                name="code"
                label="Mã định danh (Code)"
                rules={[
                  { required: true, message: 'Vui lòng nhập mã định danh' },
                  { max: 80, message: 'Tối đa 80 ký tự' },
                  {
                    pattern: /^[a-zA-Z0-9_]+$/,
                    message: 'Mã chỉ gồm chữ cái tiếng Anh, số và dấu gạch dưới',
                  },
                ]}
                extra="Viết hoa, không dấu (vd: CROHN_DISEASE, SUCRALFATE)"
              >
                <Input
                  placeholder="vd: CROHN_DISEASE"
                  className="font-mono uppercase"
                  onChange={(e) => {
                    const val = e.target.value.toUpperCase().replace(/\s+/g, '_')
                    defineCodeForm.setFieldValue('code', val)
                  }}
                />
              </Form.Item>

              <Form.Item
                name="name"
                label="Tên tiếng Việt hiển thị"
                rules={[{ required: true, message: 'Vui lòng nhập tên tiếng Việt' }]}
                extra="Tên thân thiện cho bệnh nhân & bác sĩ"
              >
                <Input placeholder="vd: Bệnh Crohn (Viêm ruột từng vùng)" />
              </Form.Item>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Form.Item
                name="source"
                label="Nguồn chuẩn hóa / Tag tham chiếu"
                rules={[{ required: true, message: 'Vui lòng nhập nguồn hoặc tag' }]}
                extra="Hiển thị dưới dạng huy hiệu nhãn (Tag) bên cạnh mã"
              >
                <Input placeholder="vd: ICD-10: K50, Dược thư QG, Mayo Clinic..." />
              </Form.Item>

              <Form.Item
                name="sourceType"
                label="Màu sắc huy hiệu (Tag Type)"
                rules={[{ required: true, message: 'Vui lòng chọn kiểu tag' }]}
              >
                <Radio.Group className="w-full grid grid-cols-2 gap-2">
                  <Radio.Button value="icd" className="text-center text-xs">
                    🔵 ICD-10 (Xanh)
                  </Radio.Button>
                  <Radio.Button value="triage" className="text-center text-xs">
                    🔴 Cấp cứu (Đỏ)
                  </Radio.Button>
                  <Radio.Button value="drug" className="text-center text-xs">
                    🟢 Dược thư (Lá)
                  </Radio.Button>
                  <Radio.Button value="custom" className="text-center text-xs">
                    🟣 Tùy chỉnh (Tím)
                  </Radio.Button>
                </Radio.Group>
              </Form.Item>
            </div>

            <Form.Item
              name="desc"
              label="Ý nghĩa y khoa ngắn gọn"
              rules={[{ required: true, message: 'Vui lòng nhập ý nghĩa ngắn gọn' }]}
              extra="Giải thích cơ chế bệnh học, tác dụng dược lý hoặc dấu hiệu cảnh báo"
            >
              <Input.TextArea
                rows={2}
                placeholder="vd: Bệnh viêm ruột từng vùng mạn tính gây tổn thương niêm mạc ống tiêu hóa"
              />
            </Form.Item>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Form.Item
                name="scope"
                label="Phục vụ màn hình nào (Phạm vi sử dụng)"
                rules={[{ required: true, message: 'Vui lòng nhập phạm vi sử dụng' }]}
                extra="Màn hình người dùng hoặc hệ thống áp dụng mã này"
              >
                <Select
                  showSearch
                  allowClear
                  options={[
                    { value: 'Hồ sơ bệnh án & Bài viết y khoa', label: 'Hồ sơ bệnh án & Bài viết y khoa' },
                    { value: 'Hồ sơ dùng thuốc & Nhắc uống thuốc', label: 'Hồ sơ dùng thuốc & Nhắc uống thuốc' },
                    { value: 'Triage tầng 1 & Cảnh báo khẩn cấp 115', label: 'Triage tầng 1 & Cảnh báo khẩn cấp 115' },
                    { value: 'Nhật ký ăn uống & Theo dõi phân Bristol', label: 'Nhật ký ăn uống & Theo dõi phân Bristol' },
                  ]}
                  placeholder="Chọn hoặc nhập màn hình phục vụ"
                />
              </Form.Item>

              <Form.Item
                name="defaultTriageCode"
                label="Mã Triage liên kết mặc định"
                extra="Tùy chọn: Gắn cờ cấp cứu tầng 1"
              >
                <Select
                  showSearch
                  allowClear
                  options={TRIAGE_OPTIONS}
                  placeholder="Không liên kết (hoặc chọn nhóm)"
                />
              </Form.Item>
            </div>

            {/* Bản xem trước trực tiếp */}
            <div className="p-3 bg-slate-100 dark:bg-slate-900/80 rounded-lg border border-slate-200 dark:border-slate-800 text-xs">
              <div className="font-semibold text-slate-700 dark:text-slate-200 mb-1.5 flex items-center gap-1.5">
                👁️ Bản xem trước hiển thị trong giao diện:
              </div>
              <div className="flex items-center justify-between py-1 px-2.5 rounded bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 mb-2">
                <div className="flex items-center gap-1.5 overflow-hidden">
                  <span className="font-mono font-bold text-slate-800 dark:text-slate-100">
                    {defineWatchedCode || 'MA_DINH_DANH'}
                  </span>
                  <span className="text-slate-500 text-xs truncate">
                    · {defineWatchedName || 'Tên hiển thị tiếng Việt'}
                  </span>
                </div>
                <Tag
                  color={
                    defineWatchedSourceType === 'triage'
                      ? 'volcano'
                      : defineWatchedSourceType === 'drug'
                        ? 'green'
                        : defineWatchedSourceType === 'custom'
                          ? 'purple'
                          : 'blue'
                  }
                  className="text-[10px] m-0 shrink-0 border-0"
                >
                  {defineWatchedSource || 'Tag tham chiếu'}
                </Tag>
              </div>
              <div className="text-slate-600 dark:text-slate-300">
                <strong>Ý nghĩa: </strong>{defineWatchedDesc || 'Chưa nhập ý nghĩa'}
              </div>
              <div className="text-slate-500 dark:text-slate-400 mt-0.5">
                <strong>Phục vụ màn hình: </strong>{defineWatchedScope || 'Chưa chọn'}
              </div>
            </div>
          </Form>
        </Modal>
      </div>
    </CmsAppShell>
  )
}
