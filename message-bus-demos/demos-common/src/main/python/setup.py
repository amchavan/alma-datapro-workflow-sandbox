from setuptools import setup

dependencies = ['drawsmb']

setup(name='drawsmb-demos-common',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['alma', 'alma/obops/draws/examples/common'],
      install_requires=dependencies,
      zip_safe=False)
