import { ChatContainer } from '../../components/chat/ChatContainer'
import { AppShell } from '../../components/layout/AppShell'

export default function ChatPage() {
  return (
    <AppShell fixedViewport noPadding>
      <ChatContainer />
    </AppShell>
  )
}
