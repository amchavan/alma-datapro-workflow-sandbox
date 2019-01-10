from setuptools import setup

dependencies = ['drawsmb', 'drawsmb-demos-common']

setup(name='drawsmb-demos-sender',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/examples'],
      install_requires=dependencies,
      zip_safe=False)
