import React from 'react';
import { withRouter } from 'react-router-dom';
import { Button } from 'choerodon-ui/pro';
import { Page, Content, Breadcrumb } from '@choerodon/boot';
import { useHzeroPageStore } from './stores';

import './index.less';

const HzeroPage = withRouter(((props) => {
  const {
    prefixCls,
    intlPrefix,
    intl: { formatMessage },
    pageType,
    onClick,
  } = useHzeroPageStore();

  function handleClick() {
    const url = {
      user: 'hiam/user-group-management',
      role: 'hiam/role/list',
      menu: 'hiam/menu',
      instance: 'hadm/instance',
      api: 'hadm/api-overview',
      'api-test': 'hadm/api-test',
    };
    // eslint-disable-next-line no-underscore-dangle
    window.open(`${window._env_.HZERO_FRONT || process.env.HZERO_FRONT}/${url[pageType]}`);
  }

  return (
    <Page>
      <Breadcrumb />
      <Content className={`${prefixCls}`}>
        <div className={`${prefixCls}-wrap`}>
          <div className={`${prefixCls}-image`} />
          <div className={`${prefixCls}-text`}>
            <div className={`${prefixCls}-title`}>
              {formatMessage({ id: `${intlPrefix}.${pageType}.title` })}
            </div>
            <div className={`${prefixCls}-des`}>
              {formatMessage({ id: `${intlPrefix}.${pageType}.describe` })}
            </div>
            <Button
              color="primary"
              onClick={onClick || handleClick}
              funcType="raised"
            >
              {formatMessage({ id: `${intlPrefix}.link` })}
            </Button>
          </div>
        </div>
      </Content>
    </Page>
  );
}));

export default HzeroPage;
