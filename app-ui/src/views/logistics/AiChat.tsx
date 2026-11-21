import { useMemo, useRef, useState, useEffect } from 'react'
import { useChat } from '@ai-sdk/react'
import { DefaultChatTransport } from 'ai'
import Card from '@/components/ui/Card'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import Tag from '@/components/ui/Tag'
import Alert from '@/components/ui/Alert'
import Spinner from '@/components/ui/Spinner'
import appConfig from '@/configs/app.config'
import { TOKEN_NAME_IN_STORAGE } from '@/constants/api.constant'
import type { ToolCallEvent } from '@/@types/logistics'

const SESSION_STORAGE_KEY = 'logistics.chat.sessionId'

/** One conversation thread per browser tab, persisted so a reload keeps its context. */
const resolveSessionId = (): string => {
    const existing = localStorage.getItem(SESSION_STORAGE_KEY)
    if (existing) {
        return existing
    }
    const created = crypto.randomUUID()
    localStorage.setItem(SESSION_STORAGE_KEY, created)
    return created
}

type ChatPart = {
    type: string
    text?: string
    data?: ToolCallEvent
}

/**
 * FR01/FR02/FR03 — the AI Chat module.
 *
 * Streams over the AI SDK UI Message Stream Protocol v1. The tool panel on the right renders the
 * custom `data-toolCall` parts the backend merges into the same stream, which is the report's
 * "AI Tool Calling execution" visualisation backed by real data rather than a mock.
 */
const AiChat = () => {
    const [input, setInput] = useState('')
    const sessionId = useMemo(resolveSessionId, [])
    const bottomRef = useRef<HTMLDivElement>(null)

    const transport = useMemo(
        () =>
            new DefaultChatTransport({
                api: `${appConfig.apiPrefix}/chat`,
                headers: (): Record<string, string> => {
                    // The streaming endpoint sits behind the same JWT filter as every other
                    // /api/** route, so the bearer token has to ride along here too.
                    const token = localStorage.getItem(TOKEN_NAME_IN_STORAGE)
                    return token ? { Authorization: `Bearer ${token}` } : {}
                },
                // The SDK would post { id, messages, trigger }; the backend's ChatRequest is
                // { message, sessionId }. Reshaping here keeps the documented API contract
                // intact instead of bending the backend to the SDK's envelope.
                prepareSendMessagesRequest: ({ messages }) => {
                    const last = messages[messages.length - 1]
                    const text = (last?.parts ?? [])
                        .filter((part) => part.type === 'text')
                        .map((part) => (part as { text: string }).text)
                        .join('')
                    return { body: { message: text, sessionId } }
                },
            }),
        [sessionId],
    )

    const { messages, sendMessage, status, error } = useChat({ transport })

    const busy = status === 'submitted' || status === 'streaming'

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages, status])

    const toolCalls: ToolCallEvent[] = messages.flatMap((message) =>
        (message.parts as ChatPart[])
            .filter((part) => part.type === 'data-toolCall' && part.data)
            .map((part) => part.data as ToolCallEvent),
    )

    const submit = () => {
        const text = input.trim()
        if (!text || busy) {
            return
        }
        setInput('')
        sendMessage({ text })
    }

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <Card className="lg:col-span-2 flex flex-col" bodyClass="flex flex-col h-full">
                <div className="mb-4">
                    <h4>AI assistant</h4>
                    <p className="text-sm">
                        Ask about jobs, vehicles and route plans in plain language
                    </p>
                </div>

                {error && (
                    <Alert type="danger" showIcon className="mb-4">
                        {error.message}
                    </Alert>
                )}

                <div className="flex-1 overflow-y-auto min-h-100 max-h-150 pr-1">
                    {messages.length === 0 && (
                        <div className="h-full flex items-center justify-center text-center px-6">
                            <div>
                                <p className="mb-2 font-semibold">
                                    Try asking:
                                </p>
                                <p className="text-sm">
                                    &ldquo;What vehicles are available?&rdquo;
                                </p>
                                <p className="text-sm">
                                    &ldquo;Create a delivery job for 12 Jalan Ampang, high
                                    priority.&rdquo;
                                </p>
                                <p className="text-sm">
                                    &ldquo;Plan a route covering jobs 1, 2 and 3.&rdquo;
                                </p>
                            </div>
                        </div>
                    )}

                    {messages.map((message) => {
                        const text = (message.parts as ChatPart[])
                            .filter((part) => part.type === 'text')
                            .map((part) => part.text ?? '')
                            .join('')
                        if (!text) {
                            return null
                        }
                        const mine = message.role === 'user'
                        return (
                            <div
                                key={message.id}
                                className={`flex mb-3 ${mine ? 'justify-end' : 'justify-start'}`}
                            >
                                <div
                                    className={`rounded-xl px-4 py-2 max-w-[80%] whitespace-pre-wrap ${
                                        mine
                                            ? 'bg-primary text-neutral'
                                            : 'bg-gray-100 dark:bg-gray-700'
                                    }`}
                                >
                                    {text}
                                </div>
                            </div>
                        )
                    })}

                    {busy && (
                        <div className="flex items-center gap-2 text-sm">
                            <Spinner size={16} />
                            <span>Thinking…</span>
                        </div>
                    )}
                    <div ref={bottomRef} />
                </div>

                <div className="flex gap-2 mt-4">
                    <Input
                        value={input}
                        placeholder="Ask the route planning assistant…"
                        disabled={busy}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault()
                                submit()
                            }
                        }}
                    />
                    <Button variant="solid" disabled={busy} onClick={submit}>
                        Send
                    </Button>
                </div>
            </Card>

            <Card>
                <h5 className="mb-1">Tool calls</h5>
                <p className="text-sm mb-4">
                    Backend functions the assistant invoked
                </p>
                {toolCalls.length === 0 ? (
                    <p className="text-sm">
                        Nothing yet. Tool calls appear here as the assistant runs them.
                    </p>
                ) : (
                    <div className="flex flex-col gap-3">
                        {toolCalls.map((call, index) => (
                            <div
                                key={`${call.toolName}-${index}`}
                                className="border border-gray-200 dark:border-gray-600 rounded-lg p-3"
                            >
                                <div className="flex items-center justify-between mb-2">
                                    <span className="font-semibold text-sm">
                                        {call.toolName}
                                    </span>
                                    <Tag
                                        className={
                                            call.error
                                                ? 'bg-red-200 dark:text-gray-900'
                                                : 'bg-emerald-200 dark:text-gray-900'
                                        }
                                    >
                                        {call.error ? 'failed' : 'ok'}
                                    </Tag>
                                </div>
                                {Object.keys(call.arguments ?? {}).length > 0 && (
                                    <pre className="text-xs overflow-x-auto mb-2">
                                        {JSON.stringify(call.arguments, null, 2)}
                                    </pre>
                                )}
                                <p className="text-xs">
                                    {call.error ?? call.result}
                                </p>
                            </div>
                        ))}
                    </div>
                )}
            </Card>
        </div>
    )
}

export default AiChat
