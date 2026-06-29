export interface TwxServiceOptions {
  baseUrl?: string;
  xsrfToken?: string;
}

export class TwxService {
  private baseUrl: string;
  private xsrfToken: string;

  constructor(options: TwxServiceOptions = {}) {
    this.baseUrl = options.baseUrl || '/Thingworx';
    this.xsrfToken = options.xsrfToken || 'TWX-XSRF-TOKEN-VALUE';
  }

  private getHeaders(): Record<string, string> {
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-XSRF-TOKEN': this.xsrfToken,
      'X-Requested-By': 'ThingWorx',
    };
  }

  async invokeService<T = unknown>(
    thingName: string,
    serviceName: string,
    params: Record<string, unknown> = {}
  ): Promise<T> {
    const url = `${this.baseUrl}/Things/${encodeURIComponent(thingName)}/Services/${encodeURIComponent(serviceName)}`;
    const resp = await fetch(url, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(params),
    });
    if (!resp.ok) {
      const text = await resp.text();
      throw new Error(`Service call failed: ${resp.status} - ${text}`);
    }
    const ct = resp.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      return resp.json() as Promise<T>;
    }
    return resp.text() as unknown as Promise<T>;
  }

  async invokeServiceOnThing<T = unknown>(
    thingName: string,
    serviceName: string,
    params: Record<string, unknown> = {}
  ): Promise<T> {
    return this.invokeService<T>(thingName, serviceName, params);
  }

  async getProperty<T = unknown>(
    thingName: string,
    propertyName: string
  ): Promise<T> {
    const url = `${this.baseUrl}/Things/${encodeURIComponent(thingName)}/Properties/${encodeURIComponent(propertyName)}`;
    const resp = await fetch(url, {
      method: 'GET',
      headers: this.getHeaders(),
    });
    if (!resp.ok) throw new Error(`Property get failed: ${resp.status}`);
    return resp.json() as Promise<T>;
  }

  async setProperty(
    thingName: string,
    propertyName: string,
    value: unknown
  ): Promise<void> {
    const url = `${this.baseUrl}/Things/${encodeURIComponent(thingName)}/Properties/${encodeURIComponent(propertyName)}`;
    const resp = await fetch(url, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(value),
    });
    if (!resp.ok) throw new Error(`Property set failed: ${resp.status}`);
  }

  async queryEntities<T = unknown>(
    entityType: string,
    filters?: Record<string, unknown>
  ): Promise<T> {
    const params = filters || {};
    return this.invokeService<T>('GIT.Utility.Thing', 'QueryThings', params);
  }
}

export const twx = new TwxService();
