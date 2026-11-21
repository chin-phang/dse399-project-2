import {
    NAV_ITEM_TYPE_TITLE,
    NAV_ITEM_TYPE_ITEM,
} from '@/constants/navigation.constant'

import type { NavigationTree } from '@/@types/navigation'

/** The four modules of the route planning assistant. */
const navigationConfig: NavigationTree[] = [
    {
        key: 'logistics',
        path: '',
        title: 'Route Planning',
        translateKey: '',
        icon: '',
        type: NAV_ITEM_TYPE_TITLE,
        authority: [],
        subMenu: [],
    },
    {
        key: 'dashboard',
        path: '/dashboard',
        title: 'Dashboard',
        translateKey: '',
        icon: 'home',
        type: NAV_ITEM_TYPE_ITEM,
        authority: [],
        subMenu: [],
    },
    {
        key: 'aiChat',
        path: '/ai-chat',
        title: 'AI Assistant',
        translateKey: '',
        icon: 'singleMenu',
        type: NAV_ITEM_TYPE_ITEM,
        authority: [],
        subMenu: [],
    },
    {
        key: 'jobs',
        path: '/jobs',
        title: 'Delivery Jobs',
        translateKey: '',
        icon: 'groupSingleMenu',
        type: NAV_ITEM_TYPE_ITEM,
        authority: [],
        subMenu: [],
    },
    {
        key: 'vehicles',
        path: '/vehicles',
        title: 'Vehicles',
        translateKey: '',
        icon: 'groupCollapseMenu',
        type: NAV_ITEM_TYPE_ITEM,
        authority: [],
        subMenu: [],
    },
]

export default navigationConfig
